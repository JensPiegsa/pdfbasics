package pdfbasics;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfLT {

	private static final Logger LOG = Logger.getLogger(PdfLT.class.getName());

	@TempDir Path tempDir;

	@Test @DisplayName("test")
	void test() throws IOException, InterruptedException {
		
		final InputStream firstFileContent = getClass().getResourceAsStream("sample-01.pdf");
		final InputStream secondFileContent = getClass().getResourceAsStream("sample-02.pdf");

		final InputStream merged = merge(List.of(firstFileContent, secondFileContent));

		Files.copy(merged, tempDir.resolve("merged.pdf"));
		
		MINUTES.sleep(10);
	}

	/**
	 * Creates a compound PDF document from a list of input documents.
	 * <p>
	 * The merged document is PDF/A-1b compliant, provided the source documents are as well. It
	 * contains document properties title, creator and subject, currently hard-coded.
	 *
	 * @param sources list of source PDF document streams.
	 * @return compound PDF document as a readable input stream.
	 * @throws IOException if anything goes wrong during PDF merge.
	 */
	public InputStream merge(final List<InputStream> sources) throws IOException {
		final String title = "";
		final String creator = "";
		final String subject = "";

		try (final COSStream cosStream = new COSStream();
		     final ByteArrayOutputStream mergedPDFOutputStream = new ByteArrayOutputStream()) {
			
			// If you're merging in a servlet, you can modify this example to use the outputStream only
			// as the response as shown here: http://stackoverflow.com/a/36894346/535646

			final PDFMergerUtility pdfMerger = createPDFMergerUtility(sources, mergedPDFOutputStream);

			// PDF and XMP properties must be identical, otherwise document is not PDF/A compliant
			final PDDocumentInformation pdfDocumentInfo = createPDFDocumentInfo(title, creator, subject);
			final PDMetadata xmpMetadata = createXMPMetadata(cosStream, title, creator, subject);
			pdfMerger.setDestinationDocumentInformation(pdfDocumentInfo);
			pdfMerger.setDestinationMetadata(xmpMetadata);

			LOG.info("Merging " + sources.size() + " source documents into one PDF");
			pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			LOG.info("PDF merge successful, size = {" + mergedPDFOutputStream.size() + "} bytes");

			return new ByteArrayInputStream(mergedPDFOutputStream.toByteArray());
		} catch (final BadFieldValueException | TransformerException e) {
			throw new IOException("PDF merge problem", e);
		} finally {
			sources.forEach(IOUtils::closeQuietly);
		}
	}

	private PDFMergerUtility createPDFMergerUtility(final List<InputStream> sources, final ByteArrayOutputStream mergedPDFOutputStream) {
		LOG.info("Initialising PDF merge utility");
		final PDFMergerUtility pdfMerger = new PDFMergerUtility();
		pdfMerger.addSources(sources);
		pdfMerger.setDestinationStream(mergedPDFOutputStream);
		return pdfMerger;
	}

	private PDDocumentInformation createPDFDocumentInfo(final String title, final String creator, final String subject) {
		LOG.info("Setting document info (title, author, subject) for merged PDF");
		final PDDocumentInformation documentInformation = new PDDocumentInformation();
		documentInformation.setTitle(title);
		documentInformation.setCreator(creator);
		documentInformation.setSubject(subject);
		return documentInformation;
	}

	private PDMetadata createXMPMetadata(final COSStream cosStream, final String title, final String creator, final String subject)
			throws BadFieldValueException, TransformerException, IOException {
				
		LOG.info("Setting XMP metadata (title, author, subject) for merged PDF");
		final XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();

		// PDF/A-1b properties
		final PDFAIdentificationSchema pdfaSchema = xmpMetadata.createAndAddPFAIdentificationSchema();
		pdfaSchema.setPart(1);
		pdfaSchema.setConformance("B");

		// Dublin Core properties
		final DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
		dublinCoreSchema.setTitle(title);
		dublinCoreSchema.addCreator(creator);
		dublinCoreSchema.setDescription(subject);

		// XMP Basic properties
		final XMPBasicSchema basicSchema = xmpMetadata.createAndAddXMPBasicSchema();
		final Calendar creationDate = Calendar.getInstance();
		basicSchema.setCreateDate(creationDate);
		basicSchema.setModifyDate(creationDate);
		basicSchema.setMetadataDate(creationDate);
		basicSchema.setCreatorTool(creator);

		// Create and return XMP data structure in XML format
		try (final ByteArrayOutputStream xmpOutputStream = new ByteArrayOutputStream();
		     final OutputStream cosXMPStream = cosStream.createOutputStream()) {
			new XmpSerializer().serialize(xmpMetadata, xmpOutputStream, true);
			cosXMPStream.write(xmpOutputStream.toByteArray());
			return new PDMetadata(cosStream);
		}
	}
}
