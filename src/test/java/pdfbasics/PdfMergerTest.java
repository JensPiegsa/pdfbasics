package pdfbasics;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("A PdfMerger")
class PdfMergerTest {

	private final PdfMerger pdfMerger = new PdfMerger();
	
	@TempDir Path tempDir;

	@Test @DisplayName("creates file of expected size.")
	void createsFileOfExpectedSize() throws IOException {
		
		final InputStream firstFileContent = getClass().getResourceAsStream("sample-01.pdf");
		final InputStream secondFileContent = getClass().getResourceAsStream("sample-02.pdf");
		final List<InputStream> fileContents = Arrays.asList(firstFileContent, secondFileContent);
		final Path outputFile = tempDir.resolve("merged.pdf");

		pdfMerger.mergeContents(fileContents, outputFile);

		then(outputFile).exists();
		then(Files.size(outputFile)).isEqualTo(673_548L);
	}

}
