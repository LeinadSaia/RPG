package com.rpgsim.client;

import com.rpgsim.common.Screen;
import com.rpgsim.common.CommonConfigurations;
import com.rpgsim.common.PrefabID;
import com.rpgsim.common.Scene;
import com.rpgsim.common.game.Input;
import com.rpgsim.common.game.KeyCode;
import com.rpgsim.common.serverpackages.InstantiatePrefabRequest;
import com.rpgsim.common.sheets.PlayerSheet;
import com.rpgsim.client.sheet.SheetFrame;
import com.rpgsim.common.sheets.SheetModel;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientGame extends Canvas implements Runnable
{
    private static final int UPS = 60;
    
    private boolean gameRunning;
    private boolean networkRunning;
    private final Thread gameThread = new Thread(this, "Client Game");
    
    private final ClientManager client;
    
    private Screen screen;
    private Input input;
    
    private final Scene scene;
    
    private SheetFrame sheetFrame;
    
    public ClientGame(ClientManager manager, int width, int height)
    {
        super.setBounds(0, 0, width, height);
        super.setBackground(Color.BLACK);
        
        this.client = manager;
        this.scene = new Scene();
        
        WindowAdapter l = new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                requestFocus();
            }
        };
        this.sheetFrame = new SheetFrame(l);
        
        input = new Input();
        super.addKeyListener(input);
        super.addMouseListener(input);
        super.addMouseMotionListener(input);
    }
    
    private void update(float dt)
    {
        if (Input.getKeyDown(KeyCode.F1))
        {
            sheetFrame.setVisible(true);
        }
        scene.updateGameObjects(client.getConnectionID(), dt);
    }
    
    private void render()
    {
        screen.begin();
        scene.renderGameObjects(client.getConnectionID(), screen);
        screen.end();
    }
    
    public void start()
    {
        networkRunning = true;
        gameThread.start();
    }
    
    public void open(SheetModel model, PlayerSheet sheet)
    {
        createBufferStrategy(3);
        screen = new Screen(getBufferStrategy(), getWidth(), getHeight());
        
        this.sheetFrame.loadSheet(model, sheet);
        
        client.sendPackage(new InstantiatePrefabRequest(Input.mousePosition(), PrefabID.MOUSE));
        gameRunning = true;
        requestFocus();
    }

    public void stop()
    {
        this.sheetFrame.dispose();
        gameRunning = false;
        networkRunning = false;
    }

    @Override
    public void run()
    {
        long lastTime = System.currentTimeMillis();
        
        float gameUpdateThreshold = 1f / UPS;
        float networkUpdateThreshold = 1f / CommonConfigurations.networkUPS;
        
        float dt = 0f;
        float ndt = 0f;
        
        while (networkRunning)
        {
            long now = System.currentTimeMillis();
            
            float timePassed = (now - lastTime) * 0.001f;
            
            ndt += timePassed;
            
            while (ndt >= networkUpdateThreshold)
            {
                client.update();
                ndt -= networkUpdateThreshold;
            }
            
            if (gameRunning)
            {
                dt += timePassed;
                
                boolean render = false;
                while (dt >= gameUpdateThreshold)
                {
                    render = true;
                    update(dt);
                    input.update();
                    dt -= gameUpdateThreshold;
                }

                if (render)
                {
                    render();
                }
                else
                {
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(ClientGame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            lastTime = now;
        }
    }

    public Input getInput()
    {
        return input;
    }

    public Scene getScene()
    {
        return scene;
    }

}