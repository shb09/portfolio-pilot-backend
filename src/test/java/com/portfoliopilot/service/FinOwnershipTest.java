package com.portfoliopilot.service;

import com.portfoliopilot.dto.FinPortfolioRequest;
import com.portfoliopilot.dto.HoldingRequest;
import com.portfoliopilot.entity.FinPortfolio;
import com.portfoliopilot.entity.Holding;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.FinPortfolioRepository;
import com.portfoliopilot.repository.HoldingRepository;
import com.portfoliopilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Ownership: every scope resolves through the JWT owner's id. */
@ExtendWith(MockitoExtension.class)
class FinOwnershipTest {

    @Mock
    private FinPortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FinPortfolioService service;

    private User alice;
    private FinPortfolio alicesPortfolio;

    @BeforeEach
    void setUp() throws Exception {
        alice = new User();
        var id = User.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(alice, 1L);
        alice.setEmail("alice@test.com");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));

        alicesPortfolio = new FinPortfolio();
        var pid = FinPortfolio.class.getDeclaredField("id");
        pid.setAccessible(true);
        pid.set(alicesPortfolio, 10L);
        alicesPortfolio.setUser(alice);
    }

    @Test
    void otherUsersPortfolioIs404() {
        when(portfolioRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class, () -> service.get("alice@test.com", 99L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void holdingsScopedToOwnedPortfolio() {
        when(portfolioRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(alicesPortfolio));
        Holding h = new Holding();
        h.setSymbol("AAPL");
        when(holdingRepository.findByPortfolioIdOrderBySymbol(10L)).thenReturn(List.of(h));

        var list = service.listHoldings("alice@test.com", 10L);

        assertEquals(1, list.size());
        assertEquals("AAPL", list.get(0).symbol());
    }

    @Test
    void holdingsOfForeignPortfolioAre404() {
        when(portfolioRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.listHoldings("alice@test.com", 99L));
    }

    @Test
    void duplicateSymbolRejected() {
        when(portfolioRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(alicesPortfolio));
        when(holdingRepository.existsByPortfolioIdAndSymbol(10L, "AAPL")).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.addHolding("alice@test.com", 10L, new HoldingRequest("aapl", null, 5.0)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void nonPositiveQuantityRejected() {
        when(portfolioRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(alicesPortfolio));

        assertThrows(ResponseStatusException.class,
                () -> service.addHolding("alice@test.com", 10L, new HoldingRequest("AAPL", null, 0.0)));
        assertThrows(ResponseStatusException.class,
                () -> service.addHolding("alice@test.com", 10L, new HoldingRequest("AAPL", null, Double.NaN)));
    }

    @Test
    void portfolioCreateAssignsOwner() {
        when(portfolioRepository.save(any(FinPortfolio.class))).thenAnswer(i -> i.getArgument(0));

        service.create("alice@test.com", new FinPortfolioRequest("Retire", null, null));

        var captor = org.mockito.ArgumentCaptor.forClass(FinPortfolio.class);
        org.mockito.Mockito.verify(portfolioRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUser().getId());
    }
}
