package com.portfoliopilot.service;

import com.portfoliopilot.dto.FinPortfolioDto;
import com.portfoliopilot.dto.FinPortfolioRequest;
import com.portfoliopilot.dto.HoldingDto;
import com.portfoliopilot.dto.HoldingRequest;
import com.portfoliopilot.entity.FinPortfolio;
import com.portfoliopilot.entity.Holding;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.FinPortfolioRepository;
import com.portfoliopilot.repository.HoldingRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Owner-scoped portfolios and holdings. Every lookup joins through the
 * JWT owner's id, so one user can never touch another's records (404 in
 * both the missing and the not-mine case — no id leakage).
 */
@Service
public class FinPortfolioService {

    private final FinPortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    public FinPortfolioService(FinPortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository, UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private FinPortfolio ownedPortfolio(Long id, Long userId) {
        return portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
    }

    private Holding ownedHolding(Long holdingId, Long portfolioId) {
        return holdingRepository.findByIdAndPortfolioId(holdingId, portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
    }

    public List<FinPortfolioDto> list(String email) {
        Long userId = owner(email).getId();
        return portfolioRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(p -> FinPortfolioDto.from(p, holdingRepository.countByPortfolioId(p.getId())))
                .toList();
    }

    @Transactional
    public FinPortfolioDto create(String email, FinPortfolioRequest request) {
        FinPortfolio portfolio = new FinPortfolio();
        portfolio.setUser(owner(email));
        apply(portfolio, request);
        FinPortfolio saved = portfolioRepository.save(portfolio);
        return FinPortfolioDto.from(saved, 0);
    }

    public FinPortfolioDto get(String email, Long id) {
        FinPortfolio portfolio = ownedPortfolio(id, owner(email).getId());
        return FinPortfolioDto.from(portfolio, holdingRepository.countByPortfolioId(portfolio.getId()));
    }

    @Transactional
    public FinPortfolioDto update(String email, Long id, FinPortfolioRequest request) {
        FinPortfolio portfolio = ownedPortfolio(id, owner(email).getId());
        apply(portfolio, request);
        FinPortfolio saved = portfolioRepository.save(portfolio);
        return FinPortfolioDto.from(saved, holdingRepository.countByPortfolioId(saved.getId()));
    }

    @Transactional
    public void delete(String email, Long id) {
        FinPortfolio portfolio = ownedPortfolio(id, owner(email).getId());
        holdingRepository.deleteAll(holdingRepository.findByPortfolioIdOrderBySymbol(portfolio.getId()));
        portfolioRepository.delete(portfolio);
    }

    private void apply(FinPortfolio portfolio, FinPortfolioRequest request) {
        portfolio.setName(request.name().trim());
        portfolio.setDescription(request.description());
        String benchmark = request.benchmarkSymbol();
        portfolio.setBenchmarkSymbol(benchmark == null || benchmark.isBlank() ? null : benchmark.trim().toUpperCase());
    }

    public List<HoldingDto> listHoldings(String email, Long portfolioId) {
        FinPortfolio portfolio = ownedPortfolio(portfolioId, owner(email).getId());
        return holdingRepository.findByPortfolioIdOrderBySymbol(portfolio.getId()).stream()
                .map(HoldingDto::from).toList();
    }

    @Transactional
    public HoldingDto addHolding(String email, Long portfolioId, HoldingRequest request) {
        FinPortfolio portfolio = ownedPortfolio(portfolioId, owner(email).getId());
        String symbol = request.symbol().trim().toUpperCase();
        if (holdingRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), symbol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Symbol already held in this portfolio");
        }
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setSymbol(symbol);
        holding.setAssetCategory(request.assetCategory() == null ? null : request.assetCategory().trim());
        holding.setQuantity(requirePositiveFinite(request.quantity(), "quantity"));
        return HoldingDto.from(holdingRepository.save(holding));
    }

    @Transactional
    public HoldingDto updateHolding(String email, Long portfolioId, Long holdingId, HoldingRequest request) {
        FinPortfolio portfolio = ownedPortfolio(portfolioId, owner(email).getId());
        Holding holding = ownedHolding(holdingId, portfolio.getId());
        String symbol = request.symbol().trim().toUpperCase();
        if (!symbol.equals(holding.getSymbol())
                && holdingRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), symbol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Symbol already held in this portfolio");
        }
        holding.setSymbol(symbol);
        holding.setAssetCategory(request.assetCategory() == null ? null : request.assetCategory().trim());
        holding.setQuantity(requirePositiveFinite(request.quantity(), "quantity"));
        return HoldingDto.from(holdingRepository.save(holding));
    }

    @Transactional
    public void deleteHolding(String email, Long portfolioId, Long holdingId) {
        FinPortfolio portfolio = ownedPortfolio(portfolioId, owner(email).getId());
        holdingRepository.delete(ownedHolding(holdingId, portfolio.getId()));
    }

    static Double requirePositiveFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a positive finite number");
        }
        return value;
    }
}
