package pdfbasics;

import static java.util.stream.Collectors.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.swing.*;

public class PdfBasicsMain extends JFrame {

	private static final Logger log = Logger.getLogger(PdfBasicsMain.class.getName());
	
	private final JList<Path> list;
	private final DefaultListModel<Path> listModel;
	private final PdfMerger pdfMerger;

	public static void main(final String[] args) {
		System.out.println("classpath: " + System.getProperty("java.class.path"));

		initLookAndFeel();

		final PdfMerger pdfMerger = new PdfMerger();
		
		final PdfBasicsMain mainWindow = new PdfBasicsMain(pdfMerger);
		mainWindow.setLocationByPlatform(true);
		mainWindow.setVisible(true);
	}

	public PdfBasicsMain(final PdfMerger pdfMerger) {
		super("PDF Basics 1.0");
		this.pdfMerger = pdfMerger;

		final Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("pdfbasics.png"));
		setIconImage(icon);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		list.setTransferHandler(new ListTransferHandler());

		final JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		getContentPane().add(scrollPane, BorderLayout.CENTER);

		final JButton clearListButton = new JButton("Clear list");
		clearListButton.addActionListener(e -> {
			listModel.clear();
		});
		
		final JButton mergeButton = new JButton("Merge PDF files");
		mergeButton.addActionListener(e -> {
			final List<Path> files = IntStream.range(0, listModel.size()).mapToObj(listModel::get).collect(toList());
			log.info(() -> "files: " + files); // TODO remove verbose logging
			final String userHome = System.getProperty("user.home");
			try {
				pdfMerger.merge(files, Paths.get(userHome, "merged_" + dateTimeSuffix() + ".pdf"));
			} catch (final IOException ioe) {
				ioe.printStackTrace();
			}
		});
		
		final JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(clearListButton);
		buttons.add(mergeButton);

		getContentPane().add(buttons, BorderLayout.SOUTH);
		
		pack();
	}

	private String dateTimeSuffix() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
	}
	
	private static void initLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JFrame.setDefaultLookAndFeelDecorated(true);
		} catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	private class ListTransferHandler extends TransferHandler {

		@Override
		public boolean canImport(final TransferSupport support) {
			final boolean dataFlavorSupported = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			log.fine(() -> "dataFlavorSupported: " + dataFlavorSupported); // TODO remove verbose logging
			return dataFlavorSupported;
		}

//		@Override
//		protected Transferable createTransferable(final JComponent c) {
//			System.out.println("TODO create transferable for " + c);
//			return super.createTransferable(c);
//		}

//		@Override
//		public int getSourceActions(final JComponent c) {
//			return TransferHandler.COPY_OR_MOVE;
//		}

		@Override
		public boolean importData(final TransferSupport support) {
			if (!support.isDrop()) {
				System.out.println("no drop");
				return false;
			}
			final JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
			final int dropIndex = dropLocation.getIndex(); // TODO insert dropped files to specific position
			final boolean insert = dropLocation.isInsert(); // TODO what is an insert location ?
			log.info(() -> "insert: " + insert); // TODO remove verbose logging
			final Transferable transferable = support.getTransferable();
			try {
				@SuppressWarnings("unchecked")
				final List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				files.stream()
						.map(File::toPath)
						.forEach(listModel::addElement);
			} catch (final UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}
	}

}
