package seokhoon.trade.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HexagonalArchitectureTest {
    private static final Path APPLICATION_SOURCE = Path.of("src/main/java/seokhoon/trade/application");

    @Test
    void applicationLayerDoesNotDependOnAdapters() throws IOException {
        List<String> violations;
        try (var files = Files.walk(APPLICATION_SOURCE)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::importsAdapter)
                    .map(Path::toString)
                    .toList();
        }

        assertThat(violations)
                .as("application classes must depend on ports, not adapters")
                .isEmpty();
    }

    private boolean importsAdapter(Path sourceFile) {
        try {
            return Files.readString(sourceFile).contains("import seokhoon.trade.adapter.");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect " + sourceFile, exception);
        }
    }
}
