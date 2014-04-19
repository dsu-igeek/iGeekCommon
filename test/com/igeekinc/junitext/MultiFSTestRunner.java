/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.junitext;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;



class PathEditor extends AbstractCellEditor
    implements TableCellEditor, ActionListener 
{
    File currentFile;
    JButton button;
    JFileChooser fileChooser;
    protected static final String EDIT = "edit";

    public PathEditor() {
        button = new JButton();
        button.setActionCommand(EDIT);
        button.addActionListener(this);
        button.setBorderPainted(false);

//      Set up the dialog that the button brings up.
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
//          The user has clicked the cell, so
//          bring up the dialog.
            button.setText(currentFile.getAbsolutePath());
            fileChooser.setSelectedFile(currentFile);
            if (fileChooser.showDialog(button, "OK") == JFileChooser.APPROVE_OPTION)
                currentFile = fileChooser.getSelectedFile();
            fireEditingStopped(); //Make the renderer reappear.

        } else { //User pressed dialog's "OK" button.

        }
    }

//  Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return currentFile.toString();
    }

//  Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
        currentFile = new File((String)value);
        button.setText((String)value);
        return button;
    }
}
public class MultiFSTestRunner extends TestRunner 
{
    
    JCheckBox selectedColumnCheckBox = new JCheckBox("Selected");
    DefaultCellEditor selectedColumnEditor = new DefaultCellEditor(selectedColumnCheckBox);
    private VolumeSelectorTableModel volumeSelectorTableModel = new VolumeSelectorTableModel();
    private PathEditor pathEditor = new PathEditor();
    
    protected JFrame createUI(String suiteName) {
        JFrame returnFrame = super.createUI(suiteName);
        Container testPanel = returnFrame.getContentPane();
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Tests", testPanel);
        returnFrame.setContentPane(tabbedPane);
        tabbedPane.addTab("Volumes", createVolumeSelectorPanel());
        return returnFrame;
        
    }
    
    protected JPanel createVolumeSelectorPanel()
    {
        JPanel returnPanel = new JPanel();
        returnPanel.setLayout(new BorderLayout());
        JTable volumeTable = new JTable(volumeSelectorTableModel) {
            private static final long serialVersionUID = 3423581856034989498L;

            public TableCellEditor getCellEditor(int row, int column)
            {
                if (column == 3)
                {
                    return selectedColumnEditor;
                }
                else
                    if (column == 2)
                    {
                        return pathEditor;
                    }
                else
                {
                    return super.getCellEditor(row, column);
                }

            }

            public TableCellRenderer getCellRenderer(int row, int column)
            {
                if (column == 3)
                {
                    return new TableCellRenderer(){
                        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            return new JCheckBox("Selected", ((Boolean)value).booleanValue());
                        };
                    };
                }
                else
                {
                    return super.getCellRenderer(row, column);
                }
            }
            
        };

        JScrollPane scroller = new JScrollPane(volumeTable);
        returnPanel.add(scroller,BorderLayout.CENTER);
        return returnPanel;
    }
    
    public static void main(String[] args) {
        try
        {
            Class.forName("com.igeekinc.junitext.FSTest");
            Class.forName("com.igeekinc.junitext.FSTestCase");
        } catch (Exception e)
        {
            // TODO: handle exception
        }
        new MultiFSTestRunner().start(args);
    }


    /**
     * Starts the TestRunner
     */
    public void start(String[] args) 
    {
        LongOpt [] longOptions = {
                new LongOpt("path", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
                new LongOpt("suite", LongOpt.REQUIRED_ARGUMENT, null, 's'),
        };
        Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        int c;

        String suiteName = "";
        while ((c = getOpt.getopt()) != -1)
        {
            switch(c)
            {
            case 'p':
                String curPath = getOpt.getOptarg();
                try
                {
                    volumeSelectorTableModel.selectPath(curPath);
                } catch (IOException e)
                {
                    System.err.println("Got IOException for "+curPath);
                    e.printStackTrace();
                }
                break;
            case 's':
                suiteName = getOpt.getOptarg();
                break;
            }
        }

        fFrame= createUI(suiteName);
        fFrame.pack();
        fFrame.setVisible(true);

        if (suiteName != null) {
            setSuite(suiteName);
            runSuite();
        }

    }
    
    public Test getTest(String suiteClassName)
    {
        Test returnTest = super.getTest(suiteClassName);
        if (returnTest instanceof FSTest)
        {
            VolumeInfo [] volumeInfo = volumeSelectorTableModel.getSelectedVolumes();
            MultiFSTestUtilities.setupFSTest((FSTest)returnTest, volumeInfo);
        }
        if (returnTest instanceof TestSuite)
            setupSuite((TestSuite)returnTest);
        return returnTest;
    }
    
    public void setupSuite(TestSuite suiteToSetup)
    {
        Enumeration tests = suiteToSetup.tests();
        while (tests.hasMoreElements())
        {
            Test myTest = (Test)tests.nextElement();
            if (myTest instanceof TestSuite)
                setupSuite((TestSuite)myTest);
            if (myTest instanceof com.igeekinc.junitext.FSTest)
            {
                VolumeInfo [] volumeInfo = volumeSelectorTableModel.getSelectedVolumes();
                MultiFSTestUtilities.setupFSTest((FSTest)myTest, volumeInfo);
            }
        }
    }
}