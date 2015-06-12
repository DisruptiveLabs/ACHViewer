/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ach.achViewer;

import com.ach.achViewer.ach.ACHFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

/**
 *
 * @author frank
 */
public class Main {

    public static void main(String args[]) {

        if (args.length > 0 && args[0].equals("--validate")) {
            ACHFile achFile;

            try {
                if (args.length > 1) {
                    achFile = loadACHFileFromFilename(args[1]);
                } else {
                    achFile = loadACHFileFromStdin();
                }
            } catch (Exception ex) {
                System.err.println("Failed to read ACH File");
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                System.exit(2);
                return;
            }

            Vector<String> messages = achFile.validate();

            if (messages.isEmpty()) {
                System.out.println("ACH File Validated");
                System.exit(0);
            } else {
                System.out.println("ACH File Validation Failed");
                for (String message : messages) {
                    System.err.println("Error: " + message);
                }
                System.exit(1);
            }
        } else {
            ACHViewer.main(args);
        }
    }

    public static ACHFile loadACHFileFromFilename(String filename) throws Exception {
        return new ACHFile(filename);
    }

    public static ACHFile loadACHFileFromStdin() throws Exception {
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String line;
        ArrayList<String> lines = new ArrayList<String>();

        while ((line = reader.readLine()) != null && line.length() != 0) {
            lines.add(line);
        }

        return new ACHFile(lines);
    }

}
