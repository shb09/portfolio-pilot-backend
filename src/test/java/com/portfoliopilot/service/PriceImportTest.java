package com.portfoliopilot.service;

import com.portfoliopilot.dto.PriceImportResult;
import com.portfoliopilot.entity.AssetPrice;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AssetPriceRepository;
import com.portfoliopilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceImportTest {

    @Mock
    private AssetPriceRepository priceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PriceService service;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        var id = User.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(user, 7L);
        user.setEmail("quant@test.com");
        org.mockito.Mockito.lenient()
                .when(userRepository.findByEmail("quant@test.com")).thenReturn(Optional.of(user));
    }

    private PriceImportResult importText(String csv) {
        return service.importCsv("quant@test.com",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validRowsWithHeaderImport() {
        when(priceRepository.existsByUserIdAndSymbolAndPriceDate(7L, "AAPL", LocalDate.of(2024, 1, 2)))
                .thenReturn(false);
        when(priceRepository.existsByUserIdAndSymbolAndPriceDate(7L, "AAPL", LocalDate.of(2024, 1, 3)))
                .thenReturn(false);

        PriceImportResult result = importText("date,symbol,close\n2024-01-02,AAPL,100.5\n2024-01-03,aapl,101\n");

        assertEquals(2, result.totalRows());
        assertEquals(2, result.imported());
        assertEquals(0, result.skippedDuplicates());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void badRowsReportedWithLineNumbers() {
        PriceImportResult result = importText(
                "2024-13-01,AAPL,100\n2024-01-02,BAD SYM!,100\n2024-01-03,AAPL,-5\n2024-01-04,AAPL,abc\n2024-01-05\n");

        assertEquals(5, result.totalRows());
        assertEquals(0, result.imported());
        assertEquals(5, result.errors().size());
        assertEquals(1, result.errors().get(0).row());
        assertEquals(5, result.errors().get(4).row());
    }

    @Test
    void futureDatesRejected() {
        String future = LocalDate.now().plusDays(30) + ",AAPL,100\n";
        PriceImportResult result = importText(future);

        assertEquals(0, result.imported());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).message().contains("future"));
    }

    @Test
    void fileDuplicatesErrorAndStoredDuplicatesSkip() {
        // Row 1 is new (saved); row 2 repeats it within the file (error);
        // row 3 is a different date already stored (skipped, not an error).
        when(priceRepository.existsByUserIdAndSymbolAndPriceDate(7L, "AAPL", LocalDate.of(2024, 1, 2)))
                .thenReturn(false);
        when(priceRepository.existsByUserIdAndSymbolAndPriceDate(7L, "AAPL", LocalDate.of(2024, 1, 3)))
                .thenReturn(true);

        PriceImportResult result = importText("2024-01-02,AAPL,100\n2024-01-02,AAPL,100\n2024-01-03,AAPL,101\n");

        assertEquals(3, result.totalRows());
        assertEquals(1, result.imported());
        assertEquals(1, result.skippedDuplicates());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).message().contains("duplicate"));
    }

    @Test
    void unknownUserRejected() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.importCsv("ghost@test.com",
                new ByteArrayInputStream("2024-01-02,AAPL,100\n".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void savedEntitiesCarryOwnerAndNormalizedSymbol() {
        when(priceRepository.existsByUserIdAndSymbolAndPriceDate(7L, "MSFT", LocalDate.of(2024, 1, 2)))
                .thenReturn(false);

        service.importCsv("quant@test.com",
                new ByteArrayInputStream("2024-01-02, msft ,200\n".getBytes(StandardCharsets.UTF_8)));

        var captor = ArgumentCaptor.forClass(List.class);
        verify(priceRepository).saveAll(captor.capture());
        AssetPrice saved = ((List<AssetPrice>) captor.getValue()).get(0);
        assertEquals("MSFT", saved.getSymbol());
        assertEquals(7L, saved.getUser().getId());
    }
}
