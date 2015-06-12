/*
 * ACHFileControlDialog.java
 *
 * Created on April 16, 2007, 7:45 PM
 */

package com.ach.achViewer;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * 
 * @author John
 */
public class ACHDialog extends javax.swing.JDialog {

	static final long serialVersionUID = 0;

	public ACHDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
	}

	public class NumericVerifier extends InputVerifier {

		private long minValue = 0;

		private long maxValue = 0;

		private String lastError = null;

		public NumericVerifier(long minValue, long maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		public boolean verify(JComponent input) {

			setLastError(null);
			String errorMessage = "Error";
			try {
				errorMessage = (input.getName() == null ? "Field" : input
						.getName())
						+ " must be "
						+ (minValue == maxValue ? "" : "between " + minValue
								+ " and ") + maxValue;
				long fieldNbr = Long.parseLong(((JTextField) input).getText());

				if (fieldNbr < minValue || fieldNbr > maxValue) {
					JOptionPane.showMessageDialog(input.getParent(),
							errorMessage, "Invalid number field",
							JOptionPane.ERROR_MESSAGE);
					setLastError(errorMessage);
					return false;
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(input.getParent(), errorMessage,
						"Invalid number field", JOptionPane.ERROR_MESSAGE);
				setLastError(errorMessage);
				return false;
			}
			return true;
		}

		/**
		 * @return Returns the lastError.
		 */
		public String getLastError() {
			return lastError;
		}

		/**
		 * @param lastError
		 *            The lastError to set.
		 */
		private void setLastError(String lastError) {
			this.lastError = lastError;
		}
	}

	public class StringVerifier extends InputVerifier {

		private int minValue = 0;

		private int maxValue = 0;

		private String lastError = null;

		public StringVerifier(int minValue, int maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		public boolean verify(JComponent input) {
			setLastError(null);
			String errorMessage = "Error";
			try {
				errorMessage = (input.getName() == null ? "Field" : input
						.getName())
						+ " must be "
						+ (minValue == maxValue ? "" : "between " + minValue
								+ " and ") + maxValue + " in size.";
				String fieldValue = ((JTextField) input).getText();
				if (fieldValue.length() < minValue
						|| fieldValue.length() > maxValue) {
					JOptionPane.showMessageDialog(input.getParent(),
							errorMessage, "Invalid field",
							JOptionPane.ERROR_MESSAGE);
					setLastError(errorMessage);
					return false;
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(input.getParent(), errorMessage,
						"Invalid field", JOptionPane.ERROR_MESSAGE);
				setLastError(errorMessage);
				return false;
			}
			return true;
		}

		/**
		 * @return Returns the lastError.
		 */
		public String getLastError() {
			return lastError;
		}

		/**
		 * @param lastError
		 *            The lastError to set.
		 */
		private void setLastError(String lastError) {
			this.lastError = lastError;
		}
	}
}
