package com.hypefree.blogspot.dyncompile;

import javax.swing.*;

public class DynCompileApplet extends JApplet {
	private static final long serialVersionUID = 1L;

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	try {
						DynCompile.doTest();
					} catch (Exception e) {
						e.printStackTrace();
					}
                	
                    JLabel lbl = new JLabel("Hello World");
                    add(lbl);
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't complete successfully");
        }
    }
}
