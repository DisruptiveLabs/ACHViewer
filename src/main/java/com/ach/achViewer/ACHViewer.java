/*
 * ACHViewer.java
 *
 * Created on April 15, 2007, 11:16 AM
 */

package com.ach.achViewer;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.ach.achViewer.ach.ACHBatch;
import com.ach.achViewer.ach.ACHEntry;
import com.ach.achViewer.ach.ACHFile;
import com.ach.achViewer.ach.ACHRecord;
import com.ach.achViewer.ach.ACHRecordAddenda;
import com.ach.achViewer.ach.ACHRecordBatchControl;
import com.ach.achViewer.ach.ACHRecordBatchHeader;
import com.ach.achViewer.ach.ACHRecordEntryDetail;
import com.ach.achViewer.ach.ACHSelection;

/**
 * 
 * @author John
 */
public class ACHViewer extends javax.swing.JFrame {

	private static final long serialVersionUID = 0;

	private ACHFile achFile = null;

	private boolean achFileDirty = false;

	// Retrieved after initializing components. Used to add an '*' to the title
	// when an ACH file has been edited.
	String title = "";

	Vector<Integer[]> positions = new Vector<Integer[]>(10, 10);

	Point mouseClick = null;

	/** Creates new form ACHViewer */
	public ACHViewer() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// Don't throw errors if the native look and feel can't be found,
			// just go with what we have
		} catch (UnsupportedLookAndFeelException e) {
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}

		initComponents();
		jMenuItemToolsRecalculate.setEnabled(false);
		jMenuItemToolsValidate.setEnabled(false);

		title = this.getTitle();

		setLocationRelativeTo(null); // Centers the window on the screen

		clearAchInfo();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent evt) {
			}

			public void windowDeactivated(WindowEvent evt) {
			}

			public void windowIconified(WindowEvent evt) {
			}

			public void windowDeiconified(WindowEvent evt) {
			}

			public void windowOpened(WindowEvent evt) {
			}

			public void windowClosing(WindowEvent evt) {
				exitProgram();
			}

			public void windowClosed(WindowEvent evt) {
			}
		});

	}

	public ACHViewer(String fileName) {
		this();
		jLabelAchInfoFileName.setText(fileName);
		loadAchData(fileName);
	}

	private void clearAchInfo() {
		jLabelAchInfoFileName.setText("");
		jLabelAchInfoFileCreation.setText("");
		jLabelAchInfoImmDest.setText("");
		jLabelAchInfoImmOrigin.setText("");
		jLabelAchInfoBatchCount.setText("0");
		jLabelAchInfoEntryCount.setText("0");
		jLabelAchInfoTotalDebit.setText("0");
		jLabelAchInfoTotalCredit.setText("0");
		jListAchDataAchRecords.setModel(new DefaultListModel());
	}

	private void loadAchData(String fileName) {

		Cursor currentCursor = this.getCursor();
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		try {
			setAchFile(new ACHFile(fileName));
			loadAchInformation();
			clearJListAchDataAchRecords();
			loadAchDataRecords();
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
		Vector<String> errorMessages = getAchFile().getErrorMessages();
		if (errorMessages.size() == 0) {
			JOptionPane.showMessageDialog(this, "File loaded without error");
		} else {
			StringBuffer errorMessage = new StringBuffer("");
			for (int i = 0; i < errorMessages.size(); i++) {
				errorMessage.append(errorMessages.get(i)
						+ System.getProperty("line.separator", "\r\n"));
			}
			JOptionPane.showMessageDialog(this, errorMessage);
		}
		setAchFileDirty(false);
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(currentCursor.getType()));
		}
	}

	private void loadAchInformation() {
		jLabelAchInfoFileCreation.setText(achFile.getFileHeader()
				.getFileCreationDate()
				+ " " + achFile.getFileHeader().getFileCreationTime());
		jLabelAchInfoBatchCount.setText(achFile.getFileControl()
				.getBatchCount());
		jLabelAchInfoEntryCount.setText(achFile.getFileControl()
				.getEntryAddendaCount());
		jLabelAchInfoTotalDebit.setText(achFile.getFileControl()
				.getTotDebitDollarAmt());
		jLabelAchInfoTotalCredit.setText(achFile.getFileControl()
				.getTotCreditDollarAmt());
		jLabelAchInfoImmDest.setText(achFile.getFileHeader()
				.getImmediateDestination()
				+ " " + achFile.getFileHeader().getImmediateDestinationName());
		jLabelAchInfoImmOrigin.setText(achFile.getFileHeader()
				.getImmediateOrigin()
				+ " " + achFile.getFileHeader().getImmediateOriginName());
		jCheckBoxMenuFedFile.setSelected(achFile.isFedFile());

	}

	private synchronized void clearJListAchDataAchRecords() {
		((DefaultListModel) jListAchDataAchRecords.getModel())
				.removeAllElements();
		jMenuItemToolsRecalculate.setEnabled(false);
		jMenuItemToolsValidate.setEnabled(false);
	}

	private synchronized void addJListAchDataAchRecordsItem(ACHRecord achRecord) {
		((DefaultListModel) jListAchDataAchRecords.getModel())
				.addElement(achRecord);
	}

	private void loadAchDataRecords() {
		positions = new Vector<Integer[]>(10, 10);
		addJListAchDataAchRecordsItem(getAchFile().getFileHeader());
		positions.add(new Integer[0]);
		Vector<ACHBatch> achBatches = getAchFile().getBatches();
		for (int i = 0; i < achBatches.size(); i++) {
			addJListAchDataAchRecordsItem(achBatches.get(i).getBatchHeader());
			positions.add(new Integer[] { i });
			Vector<ACHEntry> achEntries = achBatches.get(i).getEntryRecs();
			for (int j = 0; j < achEntries.size(); j++) {
				addJListAchDataAchRecordsItem(achEntries.get(j)
						.getEntryDetail());
				positions.add(new Integer[] { i, j });
				Vector<ACHRecordAddenda> achAddendas = achEntries.get(j)
						.getAddendaRecs();
				for (int k = 0; k < achAddendas.size(); k++) {
					addJListAchDataAchRecordsItem(achAddendas.get(k));
					positions.add(new Integer[] { i, j, k });
				}
			}
			addJListAchDataAchRecordsItem(achBatches.get(i).getBatchControl());
			positions.add(new Integer[] { i });
		}
		addJListAchDataAchRecordsItem(getAchFile().getFileControl());
		positions.add(new Integer[0]);
		jMenuItemToolsRecalculate.setEnabled(true);
		jMenuItemToolsValidate.setEnabled(true);
	}

	private void processRightClick(MouseEvent evt, int[] selected) {

		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		ACHRecord achRecord = (ACHRecord) (jListAchDataAchRecords.getModel()
				.getElementAt(itemAtMouse));
		if (selected.length == 1) {
			if (achRecord.isEntryDetailType()) {
				jPopupMenuEntry.show(jListAchDataAchRecords, mouseClick.x,
						mouseClick.y);
			} else if (achRecord.isAddendaType()) {
				jPopupMenuAddenda.show(jListAchDataAchRecords, mouseClick.x,
						mouseClick.y);
			} else if (achRecord.isBatchControlType()
					|| achRecord.isBatchHeaderType()) {
				jPopupMenuBatch.show(jListAchDataAchRecords, mouseClick.x,
						mouseClick.y);
			} else if (achRecord.isFileControlType()
					|| achRecord.isFileHeaderType()) {
				jPopupMenuFile.show(jListAchDataAchRecords, mouseClick.x,
						mouseClick.y);
			}
		} else {
			jPopupMenuMultipleSelection.show(jListAchDataAchRecords,
					mouseClick.x, mouseClick.y);
		}

	}

	// If a ACHSelection type is on the system clipboard, this method returns
	// it;
	// otherwise it returns null.
	@SuppressWarnings("unchecked")
	private Vector<ACHRecord> getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard()
				.getContents(null);

		try {
			if (t != null
					&& t.isDataFlavorSupported(ACHSelection.achRecordFlavor)) {
				Vector<ACHRecord> text = (Vector<ACHRecord>) (t
						.getTransferData(ACHSelection.achRecordFlavor));
				return text;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return null;
	}

	// This method writes the current selection to the system clipboard
	private void copy(int[] selected) {
		Vector<ACHRecord> achRecords = new Vector<ACHRecord>(selected.length,
				10);
		for (int i = 0; i < selected.length; i++) {
			achRecords.add((ACHRecord) jListAchDataAchRecords.getModel()
					.getElementAt(i));
		}
		ACHSelection clipboardSelection = new ACHSelection(achRecords);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				clipboardSelection, null);
	}

	private void editAchRecord(int selectRow) {

		char recordType = ((ACHRecord) jListAchDataAchRecords.getModel()
				.getElementAt(selectRow)).getRecordTypeCode();
		Integer[] position = positions.get(selectRow);
		if (recordType == ACHRecord.FILE_HEADER_TYPE) {
			editAchFileHeader(selectRow);
		} else if (recordType == ACHRecord.FILE_CONTROL_TYPE) {
			editAchFileControl(selectRow);
		} else if (position.length == 1) {
			if (recordType == ACHRecord.BATCH_HEADER_TYPE) {
				editAchBatchHeader(position[0], selectRow);
			} else {
				editAchBatchControl(position[0], selectRow);
			}
		} else if (position.length == 2) {
			editAchEntryDetail(position[0], position[1], selectRow);
		} else {
			editAchAddenda(position[0], position[1], position[2], selectRow);
		}
	}

	private void editAchFileHeader(int selectRow) {
		ACHFileHeaderDialog dialog = new ACHFileHeaderDialog(
				new javax.swing.JFrame(), true, achFile.getFileHeader());
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHFileHeaderDialog.SAVE_BUTTON) {
			achFile.setFileHeader(dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
			loadAchInformation();
		}
	}

	private void editAchFileControl(int selectRow) {
		ACHFileControlDialog dialog = new ACHFileControlDialog(
				new javax.swing.JFrame(), true, achFile.getFileControl());
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHFileControlDialog.SAVE_BUTTON) {
			achFile.setFileControl(dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
			loadAchInformation();
		}
	}

	private void editAchBatchHeader(int position, int selectRow) {
		ACHBatchHeaderDialog dialog = new ACHBatchHeaderDialog(
				new javax.swing.JFrame(), true, achFile.getBatches().get(
						position).getBatchHeader());
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHBatchHeaderDialog.SAVE_BUTTON) {
			achFile.getBatches().get(position).setBatchHeader(
					dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
		}

	}

	private void addAchBatch() {
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length < 1) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot add entry detail",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
				.getElementAt(selected[0]);
		if (achRecord.isFileHeaderType() || achRecord.isBatchHeaderType()
				|| achRecord.isBatchControlType()) {

			// Build the ACHBatch type
			ACHBatch achBatch = new ACHBatch();
			achBatch.setBatchHeader(new ACHRecordBatchHeader());
			achBatch.setBatchControl(new ACHRecordBatchControl());
			// add one entry rec
			achBatch.addEntryRecs(new ACHEntry());
			achBatch.getEntryRecs().get(0).setEntryDetail(
					new ACHRecordEntryDetail());

			// Add to achFile
			Integer[] position = positions.get(selected[0]);
			if (position.length == 0) {
				// Adding after File Header
				achFile.getBatches().add(0, achBatch);
			} else {
				achFile.getBatches().add(position[0] + 1, achBatch);
			}
			// make sure we have to save
			setAchFileDirty(true);

			// update display
			clearJListAchDataAchRecords();
			loadAchDataRecords();
			jListAchDataAchRecords.setSelectedIndex(selected[0]);
			jListAchDataAchRecords.ensureIndexIsVisible(selected[0]);
		} else {
			JOptionPane.showMessageDialog(this,
					"Cannot add entry detail after this row",
					"Cannot perform requested function",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	private void editAchBatchControl(int position, int selectRow) {
		ACHBatchControlDialog dialog = new ACHBatchControlDialog(
				new javax.swing.JFrame(), true, achFile.getBatches().get(
						position).getBatchControl());
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHBatchControlDialog.SAVE_BUTTON) {
			achFile.getBatches().get(position).setBatchControl(
					dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
		}
	}

	private void newAchFile() {
		achFile = new ACHFile();
		achFile.addBatch(new ACHBatch());
		achFile.recalculate();

		// Update display with new data
		setAchFileDirty(false); // Don't care if these records get lost
		clearJListAchDataAchRecords();
		loadAchDataRecords();
	}

	private void addAchAddenda() {
		int selected = jListAchDataAchRecords.locationToIndex(mouseClick);
		if (selected < 0) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot add addenda",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
				.getElementAt(selected);
		if (achRecord.isEntryDetailType() || achRecord.isAddendaType()) {
			Integer[] position = positions.get(selected);
			if (position.length < 2) {
				JOptionPane.showMessageDialog(this,
						"Cannot add entry detail after this item",
						"Cannot perform request", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// build empty addenda ... don't worry about sequence here, it's
			// fixed
			// later
			ACHRecordAddenda addendaRecord = new ACHRecordAddenda();
			addendaRecord.setEntryDetailSeqNbr(achFile.getBatches().get(
					position[0]).getEntryRecs().get(position[1])
					.getEntryDetail().getTraceNumber());

			if (position.length == 2) {
				// Entry record selected ... add as the first addenda
				achFile.getBatches().get(position[0]).getEntryRecs().get(
						position[1]).getAddendaRecs().add(0, addendaRecord);
			} else {
				// Addenda record selected .. add after it
				achFile.getBatches().get(position[0]).getEntryRecs().get(
						position[1]).getAddendaRecs().add(position[2] + 1,
						addendaRecord);
			}
			// Make sure entry record has the addenda indicator set
			achFile.getBatches().get(position[0]).getEntryRecs().get(
					position[1]).getEntryDetail().setAddendaRecordInd("1");
			// Resequence all addenda records
			Vector<ACHRecordAddenda> achAddendas = achFile.getBatches().get(
					position[0]).getEntryRecs().get(position[1])
					.getAddendaRecs();
			for (int i = 0; i < achAddendas.size(); i++) {
				achAddendas.get(i).setAddendaSeqNbr(String.valueOf(i + 1));
			}
			achFile.getBatches().get(position[0]).getEntryRecs().get(
					position[1]).setAddendaRecs(achAddendas);

			// Update display with new data
			setAchFileDirty(true);
			clearJListAchDataAchRecords();
			loadAchDataRecords();
			jListAchDataAchRecords.setSelectedIndex(selected);
			jListAchDataAchRecords.ensureIndexIsVisible(selected);
		} else {
			JOptionPane.showMessageDialog(this,
					"Cannot add addenda after this row",
					"Cannot perform requested function",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	private void addAchEntryDetail() {
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length < 1) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot add entry detail",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
				.getElementAt(selected[0]);
		if (achRecord.isEntryDetailType() || achRecord.isAddendaType()
				|| achRecord.isBatchHeaderType()) {
			Integer[] position = positions.get(selected[0]);
			ACHRecordEntryDetail entryRecord = new ACHRecordEntryDetail();
			ACHEntry achEntry = new ACHEntry();
			achEntry.setEntryDetail(entryRecord);
			if (position.length == 0) {
				// problem -- this can only occur on file headers and file
				// details
				JOptionPane.showMessageDialog(this,
						"Cannot add entry detail after this row",
						"Cannot perform requested function",
						JOptionPane.ERROR_MESSAGE);
				return;
			} else if (position.length == 1) {
				// Adding after Batch Header
				achFile.getBatches().get(position[0]).getEntryRecs().add(0,
						achEntry);
			} else {
				achFile.getBatches().get(position[0]).getEntryRecs().add(
						position[1] + 1, achEntry);
			}
			setAchFileDirty(true);
			clearJListAchDataAchRecords();
			loadAchDataRecords();
			jListAchDataAchRecords.setSelectedIndex(selected[0]);
			jListAchDataAchRecords.ensureIndexIsVisible(selected[0]);
		} else {
			JOptionPane.showMessageDialog(this,
					"Cannot add entry detail after this row",
					"Cannot perform requested function",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	private void deleteAchRecord(int selectRow) {

		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length > 1) {
			int addendaCount = 0;
			int entryCount = 0;
			int batchHeaderCount = 0;
			int batchControlCount = 0;
			int fileHeaderCount = 0;
			int fileControlCount = 0;
			for (int i = 0; i < selected.length; i++) {
				ACHRecord achRecord = ((ACHRecord) jListAchDataAchRecords
						.getModel().getElementAt(selected[i]));
				if (achRecord.isAddendaType()) {
					addendaCount++;
				} else if (achRecord.isEntryDetailType()) {
					entryCount++;
				} else if (achRecord.isBatchHeaderType()) {
					batchHeaderCount++;
				} else if (achRecord.isBatchControlType()) {
					batchControlCount++;
				} else if (achRecord.isFileHeaderType()) {
					fileHeaderCount++;
				} else if (achRecord.isFileControlType()) {
					fileControlCount++;
				}
			}
			// Determine the type of delete from the outside in
			if (fileHeaderCount > 0 || fileControlCount > 0) {
				JOptionPane
						.showMessageDialog(
								this,
								"Cannot delete file header or control records. Use 'New' menu item instead",
								"Cannot perform request",
								JOptionPane.ERROR_MESSAGE);
				return;
			} else if (batchHeaderCount > 0 || batchControlCount > 0) {
				Object[] options = { "Delete", "Cancel" };
				int selection = JOptionPane
						.showOptionDialog(
								this,
								"This will delete multiple batches. Continue with delete??",
								"Deleting multiple batches",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (selection == 0) {
					deleteAchBatch();
				} else {
					return;
				}
			} else if (entryCount > 0) {
				Object[] options = { "Delete", "Cancel" };
				int selection = JOptionPane
						.showOptionDialog(
								this,
								"This will delete multiple entry details. Continue with delete??",
								"Deleting multiple entry details",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (selection == 0) {
					deleteAchEntryDetail();
				} else {
					return;
				}
			} else if (addendaCount > 0) {
				Object[] options = { "Delete", "Cancel" };
				int selection = JOptionPane
						.showOptionDialog(
								this,
								"This will delete multiple Addenda records. Continue with delete??",
								"Deleting multiple Addenda records",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (selection == 0) {
					deleteAchAddenda();
				} else {
					return;
				}
			}
		} else {
			ACHRecord achRecord = ((ACHRecord) jListAchDataAchRecords
					.getModel().getElementAt(selectRow));
			if (achRecord.isBatchHeaderType() || achRecord.isBatchControlType()) {
				deleteAchBatch();
			} else if (achRecord.isEntryDetailType()) {
				deleteAchEntryDetail();
			} else if (achRecord.isAddendaType()) {
				deleteAchAddenda();
			}
		}
	}

	private void deleteAchAddenda() {
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length < 1) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot delete entry detail",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (int i = 0; i < selected.length; i++) {
			ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
					.getElementAt(selected[i]);
			if (achRecord.isAddendaType()) {
			} else {
				JOptionPane.showMessageDialog(this,
						"Cannot delete addenda records -- non-entry/addenda rows "
								+ "in selection list",
						"Cannot perform requested function",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		// Remove them backwards to positions don't shift on us
		Integer[] position = new Integer[0];
		for (int i = selected.length - 1; i >= 0; i--) {
			position = positions.get(selected[i]);
			if (position.length != 3) {
				// problem -- this can only occur if there is a mismatch
				// between positions and jListAchDataAchRecords
				JOptionPane.showMessageDialog(this,
						"Cannot delete addenda -- row is not an addenda row",
						"Cannot perform requested function",
						JOptionPane.ERROR_MESSAGE);
				return;
			} else {
				achFile.getBatches().get(position[0]).getEntryRecs().get(
						position[1]).getAddendaRecs().remove(
						position[2].intValue());
			}
		}
		// position has the first addenda that was deleted ... we need it to
		// know where the
		// entry is
		if (position.length > 0) {

			// Make sure entry record has the addenda indicator set if there are
			// addenda recs left
			achFile.getBatches().get(position[0]).getEntryRecs().get(
					position[1]).getEntryDetail().setAddendaRecordInd(
					achFile.getBatches().get(position[0]).getEntryRecs().get(
							position[1]).getAddendaRecs().size() > 0 ? "1"
							: "0");
			// Resequence all addenda records -- this won't do anything
			// if all addenda records were deleted
			Vector<ACHRecordAddenda> achAddendas = achFile.getBatches().get(
					position[0]).getEntryRecs().get(position[1])
					.getAddendaRecs();
			for (int i = 0; i < achAddendas.size(); i++) {
				achAddendas.get(i).setAddendaSeqNbr(String.valueOf(i + 1));
			}
			achFile.getBatches().get(position[0]).getEntryRecs().get(
					position[1]).setAddendaRecs(achAddendas);
		}

		setAchFileDirty(true);
		clearJListAchDataAchRecords();
		loadAchDataRecords();
		jListAchDataAchRecords.setSelectedIndex(selected[0]);
		jListAchDataAchRecords.ensureIndexIsVisible(selected[0]);
	}

	private void deleteAchEntryDetail() {
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length < 1) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot delete entry detail",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (int i = 0; i < selected.length; i++) {
			ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
					.getElementAt(selected[i]);
			if (achRecord.isEntryDetailType() || achRecord.isAddendaType()) {
			} else {
				JOptionPane.showMessageDialog(this,
						"Cannot delete entry detail -- non-entry/addenda rows "
								+ "in selection list",
						"Cannot perform requested function",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		// Remove them backwards to positions don't shift on us
		for (int i = selected.length - 1; i >= 0; i--) {
			Integer[] position = positions.get(selected[i]);
			if (position.length != 2) {
				// problem -- this can only occur if there is a mismatch
				// between positions and jListAchDataAchRecords
				JOptionPane
						.showMessageDialog(
								this,
								"Cannot delete entry detail -- row is not an entry row",
								"Cannot perform requested function",
								JOptionPane.ERROR_MESSAGE);
				return;
			} else {
				achFile.getBatches().get(position[0]).getEntryRecs().remove(
						position[1].intValue());
			}
		}
		setAchFileDirty(true);
		clearJListAchDataAchRecords();
		loadAchDataRecords();
		jListAchDataAchRecords.setSelectedIndex(selected[0]);
		jListAchDataAchRecords.ensureIndexIsVisible(selected[0]);
	}

	private void deleteAchBatch() {
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		if (selected.length < 1) {
			JOptionPane.showMessageDialog(this,
					"No items selected ... cannot delete",
					"Cannot perform request", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (int i = 0; i < selected.length; i++) {
			ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords.getModel()
					.getElementAt(selected[i]);
			if (achRecord.isFileHeaderType() || achRecord.isFileControlType()) {
				JOptionPane.showMessageDialog(this,
						"Cannot delete file header/control rows "
								+ "in selection list",
						"Cannot perform requested function",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		// Remove them backwards to positions don't shift on us
		for (int i = selected.length - 1; i >= 0; i--) {
			Integer[] position = positions.get(selected[i]);
			if (position.length != 1) {
				// find batch headers -- skipp entry and addenda
			} else {
				ACHRecord achRecord = (ACHRecord) jListAchDataAchRecords
						.getModel().getElementAt(selected[i]);
				// only delete items that match the headers
				if (achRecord.isBatchHeaderType()) {
					achFile.getBatches().remove(position[0].intValue());
				}
			}
		}
		setAchFileDirty(true);
		clearJListAchDataAchRecords();
		loadAchDataRecords();
		jListAchDataAchRecords.setSelectedIndex(selected[0]);
		jListAchDataAchRecords.ensureIndexIsVisible(selected[0]);
	}

	private void editAchEntryDetail(int batchPosition, int entryPosition,
			int selectRow) {
		ACHEntryDetailDialog dialog = new ACHEntryDetailDialog(
				new javax.swing.JFrame(), true, achFile.getBatches().get(
						batchPosition).getEntryRecs().get(entryPosition)
						.getEntryDetail());
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHEntryDetailDialog.SAVE_BUTTON) {
			achFile.getBatches().get(batchPosition).getEntryRecs().get(
					entryPosition).setEntryDetail(dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
		}
	}

	private void editAchAddenda(int batchPosition, int entryPosition,
			int addendaPosition, int selectRow) {
		ACHAddendaDialog dialog = new ACHAddendaDialog(
				new javax.swing.JFrame(), true, achFile.getBatches().get(
						batchPosition).getEntryRecs().get(entryPosition)
						.getAddendaRecs().get(addendaPosition));
		dialog.setVisible(true);
		if (dialog.getButtonSelected() == ACHAddendaDialog.SAVE_BUTTON) {
			achFile.getBatches().get(batchPosition).getEntryRecs().get(
					entryPosition).getAddendaRecs().set(addendaPosition,
					dialog.getAchRecord());
			((DefaultListModel) jListAchDataAchRecords.getModel())
					.setElementAt(dialog.getAchRecord(), selectRow);
			setAchFileDirty(true);
		}
	}

	/**
	 * @return the achFile
	 */
	public synchronized ACHFile getAchFile() {
		return achFile;
	}

	/**
	 * @param achFile
	 *            the achFile to set
	 */
	public synchronized void setAchFile(ACHFile achFile) {
		this.achFile = achFile;
	}

	private boolean exitProgram() {
		if (isAchFileDirty()) {
			Object[] options = { "Save", "Exit", "Cancel" };
			int selection = JOptionPane.showOptionDialog(this,
					"ACH File has been changed? What would you like to do.",
					"ACH File has changed", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (selection == 2) {
				// Selected cancel
				return false;
			} else if (selection == 0) {
				try {
					achFile.setFedFile(jCheckBoxMenuFedFile.isSelected());
					if (!achFile.save(jLabelAchInfoFileName.getText())) {
						return false;
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this,
							"Failure saving file -- \n" + ex.getMessage(),
							"Error saving file", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}
		dispose();
		return true;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// ">//GEN-BEGIN:initComponents
	private void initComponents() {
		jPopupMenuEntry = new javax.swing.JPopupMenu();
		jMenuItemPopupEntryAdd = new javax.swing.JMenuItem();
		jMenuItemPopupEntryAddAddenda = new javax.swing.JMenuItem();
		jMenuItemPopupEntryDelete = new javax.swing.JMenuItem();
		jMenuItemPopupEntryEditEntry = new javax.swing.JMenuItem();
		jMenuItemPopupEntryCopy = new javax.swing.JMenuItem();
		jMenuItemPopupEntryPaste = new javax.swing.JMenuItem();
		jPopupMenuBatch = new javax.swing.JPopupMenu();
		jMenuItemPopupBatchAdd = new javax.swing.JMenuItem();
		jMenuItemPopupBatchAddEntry = new javax.swing.JMenuItem();
		jMenuItemPopupBatchDeleteBatch = new javax.swing.JMenuItem();
		jMenuItemPopupBatchEditBatch = new javax.swing.JMenuItem();
		jMenuItemPopupBatchCopy = new javax.swing.JMenuItem();
		jMenuItemPopupBatchPaste = new javax.swing.JMenuItem();
		jPopupMenuAddenda = new javax.swing.JPopupMenu();
		jMenuItemPopupEntryAddendaAdd = new javax.swing.JMenuItem();
		jMenuItemPopupAddendaDelete = new javax.swing.JMenuItem();
		jMenuItemPopupAddendaCopy = new javax.swing.JMenuItem();
		jMenuItemPopupAddendaPaste = new javax.swing.JMenuItem();
		jPopupMenuFile = new javax.swing.JPopupMenu();
		jMenuItemPopupFileAddBatch = new javax.swing.JMenuItem();
		jMenuItemPopupFileEdit = new javax.swing.JMenuItem();
		jPopupMenuMultipleSelection = new javax.swing.JPopupMenu();
		jMenuItemMulitpleDelete = new javax.swing.JMenuItem();
		jMenuItemPopupMultipleCopy = new javax.swing.JMenuItem();
		jMenuItemPopupPasteMultiple = new javax.swing.JMenuItem();
		jPanel1 = new javax.swing.JPanel();
		jLabelAchInfoFileNameText = new javax.swing.JLabel();
		jLabelAchInfoImmDestText = new javax.swing.JLabel();
		jLabelAchInfoImmOriginText = new javax.swing.JLabel();
		jLabelAchInfoFileName = new javax.swing.JLabel();
		jLabelAchInfoImmDest = new javax.swing.JLabel();
		jLabelAchInfoImmOrigin = new javax.swing.JLabel();
		jLabelAchInfoFileCreation = new javax.swing.JLabel();
		jLabelAchInfoFileCreationText = new javax.swing.JLabel();
		jLabelAchInfoBatchCountText = new javax.swing.JLabel();
		jLabelAchInfoBatchCount = new javax.swing.JLabel();
		jLabelAchInfoEntryCountText = new javax.swing.JLabel();
		jLabelAchInfoEntryCount = new javax.swing.JLabel();
		jLabelAchInfoTotalDebitText = new javax.swing.JLabel();
		jLabelAchInfoTotalCreditText = new javax.swing.JLabel();
		jLabelAchInfoTotalDebit = new javax.swing.JLabel();
		jLabelAchInfoTotalCredit = new javax.swing.JLabel();
		jScrollPane1 = new javax.swing.JScrollPane();
		jListAchDataAchRecords = new javax.swing.JList();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jMenuBar = new javax.swing.JMenuBar();
		jMenuFile = new javax.swing.JMenu();
		jMenuItemFileNew = new javax.swing.JMenuItem();
		jMenuItemFileOpen = new javax.swing.JMenuItem();
		jCheckBoxMenuFedFile = new javax.swing.JCheckBoxMenuItem();
		jMenuItemFileSave = new javax.swing.JMenuItem();
		jMenuItemFileSaveAs = new javax.swing.JMenuItem();
		jSeparatorMenuFile = new javax.swing.JSeparator();
		jMenuItemFileExit = new javax.swing.JMenuItem();
		jMenuTools = new javax.swing.JMenu();
		jMenuItemToolsValidate = new javax.swing.JMenuItem();
		jMenuItemToolsRecalculate = new javax.swing.JMenuItem();
		jMenuItemToolsReverse = new javax.swing.JMenuItem();

		jMenuItemPopupEntryAdd.setText("Add Entry Record");
		jMenuItemPopupEntryAdd
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryAddActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryAdd);

		jMenuItemPopupEntryAddAddenda.setText("Add Addenda");
		jMenuItemPopupEntryAddAddenda
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryAddAddendaActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryAddAddenda);

		jMenuItemPopupEntryDelete.setText("Delete Entry");
		jMenuItemPopupEntryDelete
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryDeleteActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryDelete);

		jMenuItemPopupEntryEditEntry.setText("Edit Entry");
		jMenuItemPopupEntryEditEntry
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryEditEntryActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryEditEntry);

		jMenuItemPopupEntryCopy.setText("Copy");
		jMenuItemPopupEntryCopy
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryCopyActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryCopy);

		jMenuItemPopupEntryPaste.setText("Paste");
		jMenuItemPopupEntryPaste
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryPasteActionPerformed(evt);
					}
				});

		jPopupMenuEntry.add(jMenuItemPopupEntryPaste);

		jMenuItemPopupBatchAdd.setText("Add new batch");
		jMenuItemPopupBatchAdd
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchAddActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchAdd);

		jMenuItemPopupBatchAddEntry.setText("Add new entry");
		jMenuItemPopupBatchAddEntry
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchAddEntryActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchAddEntry);

		jMenuItemPopupBatchDeleteBatch.setText("Delete Batch");
		jMenuItemPopupBatchDeleteBatch
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchDeleteBatchActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchDeleteBatch);

		jMenuItemPopupBatchEditBatch.setText("Edit Batch");
		jMenuItemPopupBatchEditBatch
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchEditBatchActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchEditBatch);

		jMenuItemPopupBatchCopy.setText("Copy");
		jMenuItemPopupBatchCopy
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchCopyActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchCopy);

		jMenuItemPopupBatchPaste.setText("Paste");
		jMenuItemPopupBatchPaste
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupBatchPasteActionPerformed(evt);
					}
				});

		jPopupMenuBatch.add(jMenuItemPopupBatchPaste);

		jMenuItemPopupEntryAddendaAdd.setText("Add Addenda");
		jMenuItemPopupEntryAddendaAdd
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupEntryAddendaAddActionPerformed(evt);
					}
				});

		jPopupMenuAddenda.add(jMenuItemPopupEntryAddendaAdd);

		jMenuItemPopupAddendaDelete.setText("Delete Addenda");
		jMenuItemPopupAddendaDelete
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupAddendaDeleteActionPerformed(evt);
					}
				});

		jPopupMenuAddenda.add(jMenuItemPopupAddendaDelete);

		jMenuItemPopupAddendaCopy.setText("Copy");
		jMenuItemPopupAddendaCopy
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupAddendaCopyActionPerformed(evt);
					}
				});

		jPopupMenuAddenda.add(jMenuItemPopupAddendaCopy);

		jMenuItemPopupAddendaPaste.setText("Paste");
		jMenuItemPopupAddendaPaste
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupAddendaPasteActionPerformed(evt);
					}
				});

		jPopupMenuAddenda.add(jMenuItemPopupAddendaPaste);

		jMenuItemPopupFileAddBatch.setText("Add new batch");
		jMenuItemPopupFileAddBatch
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupFileAddBatchActionPerformed(evt);
					}
				});

		jPopupMenuFile.add(jMenuItemPopupFileAddBatch);

		jMenuItemPopupFileEdit.setText("Edit File");
		jMenuItemPopupFileEdit
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupFileEditActionPerformed(evt);
					}
				});

		jPopupMenuFile.add(jMenuItemPopupFileEdit);

		jMenuItemMulitpleDelete.setText("Delete");
		jMenuItemMulitpleDelete
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemMulitpleDeleteActionPerformed(evt);
					}
				});

		jPopupMenuMultipleSelection.add(jMenuItemMulitpleDelete);

		jMenuItemPopupMultipleCopy.setText("Copy");
		jMenuItemPopupMultipleCopy
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupMultipleCopyActionPerformed(evt);
					}
				});

		jPopupMenuMultipleSelection.add(jMenuItemPopupMultipleCopy);

		jMenuItemPopupPasteMultiple.setText("Paste");
		jMenuItemPopupPasteMultiple
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemPopupPasteMultipleActionPerformed(evt);
					}
				});

		jPopupMenuMultipleSelection.add(jMenuItemPopupPasteMultiple);

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("ACH Viewer");
		setName("ACHMainFrame");
		jPanel1.setBorder(javax.swing.BorderFactory
				.createTitledBorder("ACH Information"));
		jLabelAchInfoFileNameText.setText("File Name:");

		jLabelAchInfoImmDestText.setText("Destination:");

		jLabelAchInfoImmOriginText.setText("Origin:");

		jLabelAchInfoFileName.setText("File Name");

		jLabelAchInfoImmDest
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoImmDest.setText("1234567890");

		jLabelAchInfoImmOrigin
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoImmOrigin.setText("1234567890");

		jLabelAchInfoFileCreation.setText("File Creation");

		jLabelAchInfoFileCreationText.setText("File Creation:");

		jLabelAchInfoBatchCountText.setText("Batch Count:");

		jLabelAchInfoBatchCount
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoBatchCount.setText("12,345");

		jLabelAchInfoEntryCountText.setText("Entry Count:");

		jLabelAchInfoEntryCount
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoEntryCount.setText("12,345");

		jLabelAchInfoTotalDebitText.setText("Total Debit:");

		jLabelAchInfoTotalCreditText.setText("Total Credit:");

		jLabelAchInfoTotalDebit
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoTotalDebit.setText("123,456,789.00");

		jLabelAchInfoTotalCredit
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabelAchInfoTotalCredit.setText("123,456,789.00");

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(
				jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout
				.setHorizontalGroup(jPanel1Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel1Layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(
																jPanel1Layout
																		.createSequentialGroup()
																		.addGroup(
																				jPanel1Layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								false)
																						.addComponent(
																								jLabelAchInfoBatchCountText,
																								javax.swing.GroupLayout.Alignment.LEADING,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								jLabelAchInfoFileCreationText,
																								javax.swing.GroupLayout.Alignment.LEADING,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								jLabelAchInfoFileNameText,
																								javax.swing.GroupLayout.Alignment.LEADING,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								90,
																								Short.MAX_VALUE))
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(
																				jPanel1Layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								jLabelAchInfoFileName,
																								javax.swing.GroupLayout.PREFERRED_SIZE,
																								549,
																								javax.swing.GroupLayout.PREFERRED_SIZE)
																						.addComponent(
																								jLabelAchInfoFileCreation,
																								javax.swing.GroupLayout.PREFERRED_SIZE,
																								157,
																								javax.swing.GroupLayout.PREFERRED_SIZE)
																						.addGroup(
																								jPanel1Layout
																										.createSequentialGroup()
																										.addComponent(
																												jLabelAchInfoBatchCount,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												114,
																												javax.swing.GroupLayout.PREFERRED_SIZE)
																										.addGap(
																												40,
																												40,
																												40)
																										.addGroup(
																												jPanel1Layout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.TRAILING,
																																false)
																														.addComponent(
																																jLabelAchInfoTotalDebitText,
																																javax.swing.GroupLayout.Alignment.LEADING,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																Short.MAX_VALUE)
																														.addComponent(
																																jLabelAchInfoEntryCountText,
																																javax.swing.GroupLayout.Alignment.LEADING,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																78,
																																Short.MAX_VALUE))
																										.addPreferredGap(
																												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																										.addGroup(
																												jPanel1Layout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.LEADING)
																														.addComponent(
																																jLabelAchInfoTotalDebit,
																																javax.swing.GroupLayout.PREFERRED_SIZE,
																																91,
																																javax.swing.GroupLayout.PREFERRED_SIZE)
																														.addComponent(
																																jLabelAchInfoEntryCount,
																																javax.swing.GroupLayout.PREFERRED_SIZE,
																																105,
																																javax.swing.GroupLayout.PREFERRED_SIZE))))
																		.addContainerGap())
														.addGroup(
																jPanel1Layout
																		.createSequentialGroup()
																		.addComponent(
																				jLabelAchInfoTotalCreditText,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				90,
																				Short.MAX_VALUE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				jLabelAchInfoTotalCredit,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				95,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addGap(
																				464,
																				464,
																				464))
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																jPanel1Layout
																		.createSequentialGroup()
																		.addGroup(
																				jPanel1Layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.TRAILING)
																						.addGroup(
																								javax.swing.GroupLayout.Alignment.LEADING,
																								jPanel1Layout
																										.createSequentialGroup()
																										.addComponent(
																												jLabelAchInfoImmDestText,
																												javax.swing.GroupLayout.DEFAULT_SIZE,
																												90,
																												Short.MAX_VALUE)
																										.addPreferredGap(
																												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																										.addComponent(
																												jLabelAchInfoImmDest,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												343,
																												javax.swing.GroupLayout.PREFERRED_SIZE))
																						.addGroup(
																								jPanel1Layout
																										.createSequentialGroup()
																										.addComponent(
																												jLabelAchInfoImmOriginText,
																												javax.swing.GroupLayout.DEFAULT_SIZE,
																												90,
																												Short.MAX_VALUE)
																										.addPreferredGap(
																												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																										.addComponent(
																												jLabelAchInfoImmOrigin,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												343,
																												javax.swing.GroupLayout.PREFERRED_SIZE)))
																		.addGap(
																				216,
																				216,
																				216)))));
		jPanel1Layout
				.setVerticalGroup(jPanel1Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel1Layout
										.createSequentialGroup()
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoFileNameText)
														.addComponent(
																jLabelAchInfoFileName))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoFileCreationText,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																13,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																jLabelAchInfoFileCreation))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoBatchCountText)
														.addComponent(
																jLabelAchInfoBatchCount)
														.addComponent(
																jLabelAchInfoEntryCountText)
														.addComponent(
																jLabelAchInfoEntryCount))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoTotalCreditText)
														.addComponent(
																jLabelAchInfoTotalCredit)
														.addComponent(
																jLabelAchInfoTotalDebitText)
														.addComponent(
																jLabelAchInfoTotalDebit))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoImmDestText)
														.addComponent(
																jLabelAchInfoImmDest))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												jPanel1Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jLabelAchInfoImmOriginText)
														.addComponent(
																jLabelAchInfoImmOrigin))
										.addContainerGap(
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)));

		jScrollPane1.setBorder(javax.swing.BorderFactory
				.createTitledBorder("ACH Data"));
		jListAchDataAchRecords.setFont(new java.awt.Font("Courier New", 0, 12));
		jListAchDataAchRecords
				.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mouseClicked(java.awt.event.MouseEvent evt) {
						jListAchDataAchRecordsMouseClicked(evt);
					}
				});

		jScrollPane1.setViewportView(jListAchDataAchRecords);

		jLabel1.setFont(new java.awt.Font("Courier New", 0, 12));
		jLabel1
				.setText("         1         2         3         4         5         6         7         8         9 ");

		jLabel2.setFont(new java.awt.Font("Courier New", 0, 12));
		jLabel2
				.setText("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234");

		jMenuFile.setText("File");
		jMenuItemFileNew.setText("New");
		jMenuItemFileNew.setToolTipText("Create new ACH data");
		jMenuItemFileNew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemFileNewActionPerformed(evt);
			}
		});

		jMenuFile.add(jMenuItemFileNew);

		jMenuItemFileOpen.setText("Open...");
		jMenuItemFileOpen.setToolTipText("Open new file");
		jMenuItemFileOpen
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemFileOpenActionPerformed(evt);
					}
				});

		jMenuFile.add(jMenuItemFileOpen);

		jCheckBoxMenuFedFile.setText("Output as Fed File");
		jMenuFile.add(jCheckBoxMenuFedFile);

		jMenuItemFileSave.setText("Save");
		jMenuItemFileSave
				.setToolTipText("Saves existing data overwriting current file");
		jMenuItemFileSave
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemFileSaveActionPerformed(evt);
					}
				});

		jMenuFile.add(jMenuItemFileSave);

		jMenuItemFileSaveAs.setText("Save As...");
		jMenuItemFileSaveAs.setToolTipText("Save into new file");
		jMenuItemFileSaveAs
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemFileSaveAsActionPerformed(evt);
					}
				});

		jMenuFile.add(jMenuItemFileSaveAs);

		jMenuFile.add(jSeparatorMenuFile);

		jMenuItemFileExit.setText("Exit");
		jMenuItemFileExit.setToolTipText("Exit program");
		jMenuItemFileExit
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemFileExitActionPerformed(evt);
					}
				});

		jMenuFile.add(jMenuItemFileExit);

		jMenuBar.add(jMenuFile);

		jMenuTools.setText("Tools");
		jMenuTools.setToolTipText("");
		jMenuItemToolsValidate.setText("Validate");
		jMenuItemToolsValidate
				.setToolTipText("Check the validity of the ACH file");
		jMenuItemToolsValidate
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemToolsValidateActionPerformed(evt);
					}
				});

		jMenuTools.add(jMenuItemToolsValidate);

		jMenuItemToolsRecalculate.setText("Recalculate");
		jMenuItemToolsRecalculate
				.setToolTipText("Recalculate the file and batch control totals");
		jMenuItemToolsRecalculate
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemToolsRecalculateActionPerformed(evt);
					}
				});

		jMenuTools.add(jMenuItemToolsRecalculate);

		jMenuItemToolsReverse.setText("Reverse");
		jMenuItemToolsReverse
				.setToolTipText("Reverse this ACH file and recalculate totals");
		jMenuItemToolsReverse
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jMenuItemToolsReverseActionPerformed(evt);
					}
				});

		jMenuTools.add(jMenuItemToolsReverse);

		jMenuBar.add(jMenuTools);

		setJMenuBar(jMenuBar);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																jScrollPane1,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																696,
																Short.MAX_VALUE)
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				jLabel1,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				686,
																				Short.MAX_VALUE)
																		.addContainerGap())
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				jLabel2,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				667,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addGap(
																				29,
																				29,
																				29))
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				jPanel1,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addContainerGap()))));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addComponent(
												jPanel1,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												jLabel1,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												14,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jLabel2)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												jScrollPane1,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												348, Short.MAX_VALUE)));
		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void jMenuItemToolsReverseActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemToolsReverseActionPerformed
		achFile.reverse();
		loadAchInformation();
		clearJListAchDataAchRecords();
		loadAchDataRecords();
		setAchFileDirty(true);
	}// GEN-LAST:event_jMenuItemToolsReverseActionPerformed

	private void jMenuItemPopupPasteMultipleActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupPasteMultipleActionPerformed
		Vector<ACHRecord> copySet = getClipboard();
		for (int i = 0; i < copySet.size(); i++) {
			System.err.println(copySet.get(i).toString());
		}

	}// GEN-LAST:event_jMenuItemPopupPasteMultipleActionPerformed

	private void jMenuItemPopupMultipleCopyActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupMultipleCopyActionPerformed
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		copy(selected);
	}// GEN-LAST:event_jMenuItemPopupMultipleCopyActionPerformed

	private void jMenuItemPopupAddendaPasteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupAddendaPasteActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_jMenuItemPopupAddendaPasteActionPerformed

	private void jMenuItemPopupAddendaCopyActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupAddendaCopyActionPerformed
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		copy(selected);
	}// GEN-LAST:event_jMenuItemPopupAddendaCopyActionPerformed

	private void jMenuItemPopupBatchPasteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupBatchPasteActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_jMenuItemPopupBatchPasteActionPerformed

	private void jMenuItemPopupBatchCopyActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupBatchCopyActionPerformed
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		copy(selected);
	}// GEN-LAST:event_jMenuItemPopupBatchCopyActionPerformed

	private void jMenuItemPopupEntryPasteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryPasteActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_jMenuItemPopupEntryPasteActionPerformed

	private void jMenuItemPopupEntryCopyActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryCopyActionPerformed
		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		copy(selected);
	}// GEN-LAST:event_jMenuItemPopupEntryCopyActionPerformed

	private void jMenuItemMulitpleDeleteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemMulitpleDeleteActionPerformed
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		deleteAchRecord(itemAtMouse);
	}// GEN-LAST:event_jMenuItemMulitpleDeleteActionPerformed

	private void jMenuItemPopupFileEditActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupFileEditActionPerformed
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		editAchRecord(itemAtMouse);
	}// GEN-LAST:event_jMenuItemPopupFileEditActionPerformed

	private void jMenuItemPopupFileAddBatchActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupFileAddBatchActionPerformed
		addAchBatch();
	}// GEN-LAST:event_jMenuItemPopupFileAddBatchActionPerformed

	private void jMenuItemPopupAddendaDeleteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupAddendaDeleteActionPerformed
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		deleteAchRecord(itemAtMouse);
	}// GEN-LAST:event_jMenuItemPopupAddendaDeleteActionPerformed

	private void jMenuItemPopupEntryAddendaAddActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryAddendaAddActionPerformed
		addAchAddenda();
	}// GEN-LAST:event_jMenuItemPopupEntryAddendaAddActionPerformed

	private void jMenuItemFileNewActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemFileNewActionPerformed
		if (isAchFileDirty()) {
			Object[] options = { "Save", "Continue", "Cancel" };
			int selection = JOptionPane.showOptionDialog(this,
					"ACH File has been changed. What would you like to do.",
					"ACH File has changed", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (selection == 2) {
				// Selected cancel
				return;
			} else if (selection == 0) {
				try {
					achFile.setFedFile(jCheckBoxMenuFedFile.isSelected());
					if (!achFile.save(jLabelAchInfoFileName.getText())) {
						return;
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this,
							"Failure saving file -- \n" + ex.getMessage(),
							"Error saving file", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		newAchFile();
	}// GEN-LAST:event_jMenuItemFileNewActionPerformed

	private void jMenuItemPopupBatchEditBatchActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryEditBatchActionPerformed
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		editAchRecord(itemAtMouse);
	}// GEN-LAST:event_jMenuItemPopupEntryEditBatchActionPerformed

	private void jMenuItemPopupBatchDeleteBatchActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryDeleteBatchActionPerformed
		deleteAchBatch();
	}// GEN-LAST:event_jMenuItemPopupEntryDeleteBatchActionPerformed

	private void jMenuItemPopupEntryEditEntryActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryEditEntryActionPerformed
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);
		editAchRecord(itemAtMouse);
	}// GEN-LAST:event_jMenuItemPopupEntryEditEntryActionPerformed

	private void jMenuItemPopupEntryDeleteActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryDeleteActionPerformed
		deleteAchEntryDetail();
	}// GEN-LAST:event_jMenuItemPopupEntryDeleteActionPerformed

	private void jMenuItemPopupEntryAddAddendaActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryAddAddendaActionPerformed
		addAchAddenda();
	}// GEN-LAST:event_jMenuItemPopupEntryAddAddendaActionPerformed

	private void jMenuItemPopupBatchAddEntryActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupBatchAddEntryActionPerformed
		addAchEntryDetail();
	}// GEN-LAST:event_jMenuItemPopupBatchAddEntryActionPerformed

	private void jMenuItemPopupBatchAddActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupBatchAddActionPerformed
		addAchBatch();
	}// GEN-LAST:event_jMenuItemPopupBatchAddActionPerformed

	private void jMenuItemPopupEntryAddActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemPopupEntryAddActionPerformed
		addAchEntryDetail();
	}// GEN-LAST:event_jMenuItemPopupEntryAddActionPerformed

	private void jMenuItemToolsRecalculateActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemToolsRecalculateActionPerformed
		Cursor currentCursor = this.getCursor();
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		if (!achFile.recalculate()) {
			JOptionPane.showMessageDialog(this,
					"Unable to fully recalculate ... run Validate tool");
		}
		clearJListAchDataAchRecords();
		loadAchInformation();
		loadAchDataRecords();
		setAchFileDirty(true);
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(currentCursor.getType()));
		}

	}// GEN-LAST:event_jMenuItemToolsRecalculateActionPerformed

	private void jMenuItemToolsValidateActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemToolsValidateActionPerformed
		Cursor currentCursor = this.getCursor();
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		}
		Vector<String> messages = achFile.validate();
		if (currentCursor.getType() == Cursor.DEFAULT_CURSOR) {
			this.setCursor(new Cursor(currentCursor.getType()));
		}

		if (messages.size() > 0) {
			StringBuffer messageOutput = new StringBuffer("");
			for (int i = 0; i < messages.size(); i++) {
				if (i > 0) {
					messageOutput.append("\n");
				}
				messageOutput.append(messages.get(i));
			}
			JOptionPane.showMessageDialog(this, messageOutput);
		}

	}// GEN-LAST:event_jMenuItemToolsValidateActionPerformed

	private void jMenuItemFileSaveAsActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuSaveAsActionPerformed
		JFileChooser chooser = new JFileChooser(new File(jLabelAchInfoFileName
				.getText()).getParent());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Save As");

		int returnVal = chooser.showOpenDialog(getContentPane());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fileName = chooser.getSelectedFile().getAbsolutePath();
			if (new File(fileName).exists()) {
				Object[] options = { "Yes", "No" };
				int answer = JOptionPane
						.showOptionDialog(this, "File " + fileName
								+ " already exists. Overwrite??",
								"File already exists",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[1]);
				if (answer == 1) {
					return;
				}
			}

			try {
				achFile.setFedFile(jCheckBoxMenuFedFile.isSelected());
				if (achFile.save(fileName)) {
					jLabelAchInfoFileName.setText(fileName);
					setAchFileDirty(false);
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this,
						"Unable to save ACH data to fileName. Reason: "
								+ ex.getMessage(), "Error writing file",
						JOptionPane.ERROR_MESSAGE);
			}
		}

	}// GEN-LAST:event_jMenuSaveAsActionPerformed

	private void jMenuItemFileSaveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuSaveActionPerformed
		String fileName = jLabelAchInfoFileName.getText();
		try {
			achFile.setFedFile(jCheckBoxMenuFedFile.isSelected());
			if (achFile.save(fileName)) {
				jLabelAchInfoFileName.setText(fileName);
				setAchFileDirty(false);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
					"Unable to save ACH data to fileName. Reason: "
							+ ex.getMessage(), "Error writing file",
					JOptionPane.ERROR_MESSAGE);
		}
	}// GEN-LAST:event_jMenuSaveActionPerformed

	private void jListAchDataAchRecordsMouseClicked(
			java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jListAchDataAchRecordsMouseClicked
		int clickCount = evt.getClickCount();
		int button = evt.getButton();
		mouseClick = evt.getPoint();
		int itemAtMouse = jListAchDataAchRecords.locationToIndex(mouseClick);

		int[] selected = jListAchDataAchRecords.getSelectedIndices();
		boolean found = false;
		for (int i = 0; i < selected.length && (!found); i++) {
			if (itemAtMouse == selected[i]) {
				found = true;
			}
		}
		if (!found) {
			jListAchDataAchRecords.setSelectedIndex(itemAtMouse);
			selected = jListAchDataAchRecords.getSelectedIndices();
		}

		if (selected.length < 1) {
			return;
		}
		if (clickCount == 2 && button == MouseEvent.BUTTON1) {
			editAchRecord(selected[0]);
			return;
		}

		if (clickCount == 1 && button == MouseEvent.BUTTON3) {
			processRightClick(evt, selected);
			return;
		}

	}// GEN-LAST:event_jListAchDataAchRecordsMouseClicked

	private void jMenuItemFileExitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuFileExitActionPerformed
		exitProgram();
	}// GEN-LAST:event_jMenuFileExitActionPerformed

	private void jMenuItemFileOpenActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuFileOpenActionPerformed

		if (isAchFileDirty()) {
			Object[] options = { "Save", "Continue", "Cancel" };
			int selection = JOptionPane.showOptionDialog(this,
					"ACH File has been changed. What would you like to do.",
					"ACH File has changed", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (selection == 2) {
				// Selected cancel
				return;
			} else if (selection == 0) {
				try {
					achFile.setFedFile(jCheckBoxMenuFedFile.isSelected());
					if (!achFile.save(jLabelAchInfoFileName.getText())) {
						return;
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this,
							"Failure saving file -- \n" + ex.getMessage(),
							"Error saving file", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}

		JFileChooser chooser = new JFileChooser(new File(jLabelAchInfoFileName
				.getText()).getParent());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Open");

		int returnVal = chooser.showOpenDialog(getContentPane());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			jLabelAchInfoFileName.setText(chooser.getSelectedFile()
					.getAbsolutePath());

			loadAchData(chooser.getSelectedFile().getAbsolutePath());
		}
	}// GEN-LAST:event_jMenuFileOpenActionPerformed

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		ACHViewer ui = null;
		try {
			if (args.length > 0) {
				ui = new ACHViewer(args[0]);
			} else {
				ui = new ACHViewer();
			}
			ui.setVisible(true);
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace();
			if (ui != null) {
				ui.dispose();
			}
		}

	}

	/**
	 * @param achFileDirty
	 *            The achFileDirty to set.
	 */
	private void setAchFileDirty(boolean achFileDirty) {
		this.achFileDirty = achFileDirty;
		if (isAchFileDirty()) {
			this.setTitle('*' + title);
		} else {
			this.setTitle(title);
		}
	}

	/**
	 * @return Returns the achFileDirty.
	 */
	private boolean isAchFileDirty() {
		return achFileDirty;
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JCheckBoxMenuItem jCheckBoxMenuFedFile;

	private javax.swing.JLabel jLabel1;

	private javax.swing.JLabel jLabel2;

	private javax.swing.JLabel jLabelAchInfoBatchCount;

	private javax.swing.JLabel jLabelAchInfoBatchCountText;

	private javax.swing.JLabel jLabelAchInfoEntryCount;

	private javax.swing.JLabel jLabelAchInfoEntryCountText;

	private javax.swing.JLabel jLabelAchInfoFileCreation;

	private javax.swing.JLabel jLabelAchInfoFileCreationText;

	private javax.swing.JLabel jLabelAchInfoFileName;

	private javax.swing.JLabel jLabelAchInfoFileNameText;

	private javax.swing.JLabel jLabelAchInfoImmDest;

	private javax.swing.JLabel jLabelAchInfoImmDestText;

	private javax.swing.JLabel jLabelAchInfoImmOrigin;

	private javax.swing.JLabel jLabelAchInfoImmOriginText;

	private javax.swing.JLabel jLabelAchInfoTotalCredit;

	private javax.swing.JLabel jLabelAchInfoTotalCreditText;

	private javax.swing.JLabel jLabelAchInfoTotalDebit;

	private javax.swing.JLabel jLabelAchInfoTotalDebitText;

	private javax.swing.JList jListAchDataAchRecords;

	private javax.swing.JMenuBar jMenuBar;

	private javax.swing.JMenu jMenuFile;

	private javax.swing.JMenuItem jMenuItemFileExit;

	private javax.swing.JMenuItem jMenuItemFileNew;

	private javax.swing.JMenuItem jMenuItemFileOpen;

	private javax.swing.JMenuItem jMenuItemFileSave;

	private javax.swing.JMenuItem jMenuItemFileSaveAs;

	private javax.swing.JMenuItem jMenuItemMulitpleDelete;

	private javax.swing.JMenuItem jMenuItemPopupAddendaCopy;

	private javax.swing.JMenuItem jMenuItemPopupAddendaDelete;

	private javax.swing.JMenuItem jMenuItemPopupAddendaPaste;

	private javax.swing.JMenuItem jMenuItemPopupBatchAdd;

	private javax.swing.JMenuItem jMenuItemPopupBatchAddEntry;

	private javax.swing.JMenuItem jMenuItemPopupBatchCopy;

	private javax.swing.JMenuItem jMenuItemPopupBatchDeleteBatch;

	private javax.swing.JMenuItem jMenuItemPopupBatchEditBatch;

	private javax.swing.JMenuItem jMenuItemPopupBatchPaste;

	private javax.swing.JMenuItem jMenuItemPopupEntryAdd;

	private javax.swing.JMenuItem jMenuItemPopupEntryAddAddenda;

	private javax.swing.JMenuItem jMenuItemPopupEntryAddendaAdd;

	private javax.swing.JMenuItem jMenuItemPopupEntryCopy;

	private javax.swing.JMenuItem jMenuItemPopupEntryDelete;

	private javax.swing.JMenuItem jMenuItemPopupEntryEditEntry;

	private javax.swing.JMenuItem jMenuItemPopupEntryPaste;

	private javax.swing.JMenuItem jMenuItemPopupFileAddBatch;

	private javax.swing.JMenuItem jMenuItemPopupFileEdit;

	private javax.swing.JMenuItem jMenuItemPopupMultipleCopy;

	private javax.swing.JMenuItem jMenuItemPopupPasteMultiple;

	private javax.swing.JMenuItem jMenuItemToolsRecalculate;

	private javax.swing.JMenuItem jMenuItemToolsReverse;

	private javax.swing.JMenuItem jMenuItemToolsValidate;

	private javax.swing.JMenu jMenuTools;

	private javax.swing.JPanel jPanel1;

	private javax.swing.JPopupMenu jPopupMenuAddenda;

	private javax.swing.JPopupMenu jPopupMenuBatch;

	private javax.swing.JPopupMenu jPopupMenuEntry;

	private javax.swing.JPopupMenu jPopupMenuFile;

	private javax.swing.JPopupMenu jPopupMenuMultipleSelection;

	private javax.swing.JScrollPane jScrollPane1;

	private javax.swing.JSeparator jSeparatorMenuFile;
	// End of variables declaration//GEN-END:variables

}
