/**
 * This file is a part of Angry IP Scanner source code,
 * see http://www.angryip.org/ for more information.
 * Licensed under GPLv2.
 */
package net.azib.ipscan.gui.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import net.azib.ipscan.Main;
import net.azib.ipscan.config.Labels;
import net.azib.ipscan.config.Version;
import net.azib.ipscan.core.ScanningResult;
import net.azib.ipscan.core.UserErrorException;
import net.azib.ipscan.core.state.ScanningState;
import net.azib.ipscan.core.state.StateMachine;
import net.azib.ipscan.exporters.ExportProcessor;
import net.azib.ipscan.exporters.ExportProcessor.ScanningResultFilter;
import net.azib.ipscan.exporters.Exporter;
import net.azib.ipscan.exporters.ExporterRegistry;
import net.azib.ipscan.gui.MainWindow;
import net.azib.ipscan.gui.ResultTable;
import net.azib.ipscan.gui.StatusBar;
import net.azib.ipscan.gui.feeders.RangeFeederGUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

/**
 * FileActions
 * 
 * @author Anton Keks
 */
public class ScanMenuActions {

	public static boolean isLoadedFromFile = false;

	public static final class Quit implements Listener {
		public void handleEvent(Event event) {
			event.display.getActiveShell().close();
		}
	}

	static abstract class Load implements Listener {
		private final ExporterRegistry exporterRegistry;
		private final ResultTable resultTable;
		private final StatusBar statusBar;
		private final boolean isSelection;
		private final StateMachine stateMachine;
		private MainWindow mainWindow;

		Load(ExporterRegistry exporterRegistry, ResultTable resultTable, StatusBar statusBar, StateMachine stateMachine, boolean isSelection, MainWindow mainWindow) {
			this.exporterRegistry = exporterRegistry;
			this.resultTable = resultTable;
			this.statusBar = statusBar;
			this.stateMachine = stateMachine;
			this.isSelection = isSelection;
			this.mainWindow = mainWindow;
		}

		public void handleEvent(Event event) {

			if (!stateMachine.inState(ScanningState.IDLE)) {
				// ask the user whether to save incomplete results
				MessageBox box = new MessageBox(resultTable.getShell(), SWT.ICON_WARNING);
				box.setText(Version.NAME);
				box.setMessage(Labels.getLabel("exception.ExporterException.scanningInProgress2"));
				box.open();
			}

			// create the file dialog
			FileDialog fileDialog = new FileDialog(resultTable.getShell(), SWT.OPEN);

			// gather lists of extensions and exporter names
			List<String> extensions2 = new ArrayList<String>();
			List<String> descriptions = new ArrayList<String>();
			StringBuffer labelBuffer = new StringBuffer(Labels.getLabel("title.load"));
			addFileExtensions(extensions2, descriptions, labelBuffer);

			List<String> extensions = new ArrayList<String>();
			extensions.add(extensions2.get(0));

			// initialize other stuff
			fileDialog.setText(labelBuffer.toString());
			fileDialog.setFilterExtensions(extensions.toArray(new String[extensions.size()]));
			fileDialog.setFilterNames(descriptions.toArray(new String[descriptions.size()]));

			// show the dialog and receive the filename
			String fileName = fileDialog.open();

			int i = 0;

			// check the received file name
			if (fileName != null) {
				BufferedReader br = null;
				isLoadedFromFile = true;

				try {
					List<ScanningResult> results = new ArrayList<ScanningResult>();

					String line;

					br = new BufferedReader(new FileReader(fileName));

					resultTable.removeAll();
					String originalStartIP = null;
					String startIPAfterLoad = null;
					String endIp = null;

					while ((line = br.readLine()) != null) {
						i++;
						if (i == 1) {
							continue;
						}
						String[] sp = line.split("\\s+");
						if (i == 4) {
							originalStartIP = sp[1];
							startIPAfterLoad = sp[1];
							endIp = sp[3];
						}
						
						System.out.println(i + ": " + sp.length + " : " + line);

						if (sp.length < 3 || i < 8) {
							continue;
						}
						InetAddress addr = InetAddress.getByName(sp[0]);
						startIPAfterLoad = sp[0];

						ScanningResult r = new ScanningResult(addr, 1);
						r.setType(ScanningResult.ResultType.ALIVE);
						if (sp[1].contains("[n/a]")) {
							r.setType(ScanningResult.ResultType.DEAD);
						}
						List<String> values = new ArrayList<String>();
						int j = 1;
						for (String s : sp) {
							j++;
//							if (s.contains("[n/")) {
//								values.add(null);
//							} else {
								values.add(s);
//							}
						}
						r.setValues(values.toArray());
						resultTable.addOrUpdateResultRow(r);
					}

					RangeFeederGUI.setStartIPText(startIPAfterLoad);
					RangeFeederGUI.setEndIPText(endIp);

					stateMachine.transitionToNext();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (br != null)br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		private void addFileExtensions(List<String> extensions, List<String> descriptions, StringBuffer sb) {
			sb.append(" (");
			for (Exporter exporter : exporterRegistry) {
				extensions.add("*." + exporter.getFilenameExtension());
				sb.append(exporter.getFilenameExtension()).append(", ");
				descriptions.add(Labels.getLabel(exporter.getId()));
			}
			// strip the last comma
			sb.delete(sb.length() - 2, sb.length());
			sb.append(")");
		}
	}

	static abstract class SaveResults implements Listener {
		private final ExporterRegistry exporterRegistry;
		private final ResultTable resultTable;
		private final StatusBar statusBar;
		private final boolean isSelection;
		private final StateMachine stateMachine;

		SaveResults(ExporterRegistry exporterRegistry, ResultTable resultTable, StatusBar statusBar, StateMachine stateMachine, boolean isSelection) {
			this.exporterRegistry = exporterRegistry;
			this.resultTable = resultTable;
			this.statusBar = statusBar;
			this.stateMachine = stateMachine;
			this.isSelection = isSelection;
		}

		public void handleEvent(Event event) {
			if (resultTable.getItemCount() <= 0) {
				throw new UserErrorException("commands.noResults");
			}

			if (!stateMachine.inState(ScanningState.IDLE)) {
				// ask the user whether to save incomplete results
				MessageBox box = new MessageBox(resultTable.getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
				box.setText(Version.NAME);
				box.setMessage(Labels.getLabel("exception.ExporterException.scanningInProgress"));
				if (box.open() != SWT.YES)
					return;
			}

			// create the file dialog
			FileDialog fileDialog = new FileDialog(resultTable.getShell(), SWT.SAVE);

			// gather lists of extensions and exporter names
			List<String> extensions = new ArrayList<String>();
			List<String> descriptions = new ArrayList<String>();
			StringBuffer labelBuffer = new StringBuffer(Labels.getLabel(isSelection ? "title.exportSelection" : "title.exportAll"));
			addFileExtensions(extensions, descriptions, labelBuffer);

			// initialize other stuff
			fileDialog.setText(labelBuffer.toString());
			fileDialog.setFilterExtensions(extensions.toArray(new String[extensions.size()]));
			fileDialog.setFilterNames(descriptions.toArray(new String[descriptions.size()]));

			// show the dialog and receive the filename
			String fileName = fileDialog.open();

			// check the received file name
			if (fileName != null) {
				Exporter exporter = exporterRegistry.createExporter(fileName);

				statusBar.setStatusText(Labels.getLabel("state.exporting"));

				// TODO: expose appending feature in the GUI
				ExportProcessor exportProcessor = new ExportProcessor(exporter, new File(fileName), false);

				// in case of isSelection we need to create our filter
				ScanningResultFilter filter = null;
				if (isSelection) {
					filter = new ScanningResultFilter() {
						public boolean apply(int index, ScanningResult result) {
							return resultTable.isSelected(index);
						}
					};
				}

				exportProcessor.process(resultTable.getScanningResults(), filter);

				statusBar.setStatusText(null);
			}
		}

		private void addFileExtensions(List<String> extensions, List<String> descriptions, StringBuffer sb) {
			sb.append(" (");
			for (Exporter exporter : exporterRegistry) {
				extensions.add("*." + exporter.getFilenameExtension());
				sb.append(exporter.getFilenameExtension()).append(", ");
				descriptions.add(Labels.getLabel(exporter.getId()));
			}
			// strip the last comma
			sb.delete(sb.length() - 2, sb.length());
			sb.append(")");
		}
	}

	public static final class LoadFromFile extends Load {
		public LoadFromFile(ExporterRegistry exporterRegistry, ResultTable resultTable, StatusBar statusBar, StateMachine stateMachine, MainWindow mainWindow) {
			super(exporterRegistry, resultTable, statusBar, stateMachine, false, mainWindow);
		}
	}

	public static final class SaveAll extends SaveResults {
		public SaveAll(ExporterRegistry exporterRegistry, ResultTable resultTable, StatusBar statusBar, StateMachine stateMachine) {
			super(exporterRegistry, resultTable, statusBar, stateMachine, false);
		}
	}

	public static final class SaveSelection extends SaveResults {
		public SaveSelection(ExporterRegistry exporterRegistry, ResultTable resultTable, StatusBar statusBar, StateMachine stateMachine) {
			super(exporterRegistry, resultTable, statusBar, stateMachine, true);
		}
	}

	public static final class NewWindow implements Listener {
		public void handleEvent(Event event) {
			// start another instance in a new thread
			// doesn't currently work...
			new Thread("main") {
				public void run() {					
					Main.main();
				}
			}.start();
		}
	}

}