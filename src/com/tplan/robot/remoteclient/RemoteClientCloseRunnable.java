/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009  T-Plan Limited (http://www.t-plan.co.uk),
 * Tolvaddon Energy Park, Cornwall, TR14 0HX, United Kingdom
 */
package com.tplan.robot.remoteclient;

import java.io.IOException;

/**
 * Helper runnable allowing to close connection to a server inside a thread.
 *
 * @product.signature
 */
public class RemoteClientCloseRunnable implements Runnable {

    private RemoteDesktopClient client;

    public RemoteClientCloseRunnable(RemoteDesktopClient client) {
        this.client = client;
    }

    public void run() {
        try {
            client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
