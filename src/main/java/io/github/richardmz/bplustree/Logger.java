/**
 * Copyright [2025] [Chen Li]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.richardmz.bplustree;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger implements Runnable
{
    private final LinkedBlockingQueue<String> queue;
    private static Logger instance;
    private final Thread thread;
    private volatile boolean run;

    private final Level level;

    public enum Level
    {
        DEBUG, INFO, ERROR
    }

    private Logger(Level level)
    {
        this.queue = new LinkedBlockingQueue<>(5);
        this.thread = new Thread(this);
        this.level = level;
    }


    // =================================================================================================================

    public static Logger getInstance()
    {
        if (instance == null)
        {
            instance = new Logger(Level.INFO);
        }

        return instance;
    }

    public static Logger getInstance(Level level)
    {
        if (instance == null)
        {
            instance = new Logger(level);
        }
        else
        {
            System.out.println("Level is already set to " + instance.level);
        }
        return instance;
    }


    // =================================================================================================================

    public void info(String string)
    {
        if (!level.equals(Level.ERROR))
        {
            putWithTime("INFO - " + string + "\n");
        }
    }

    public void debug(String string)
    {
        debug(string, true);
    }

    public void debug(String string, boolean lineBreak)
    {
        if (level.equals(Level.DEBUG))
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DEBUG - ").append(string);
            if (lineBreak)
            {
                stringBuilder.append('\n');
            }
            putWithTime(stringBuilder.toString());
        }
    }

    public void error(String string)
    {
        putWithTime("ERROR - " + string + "\n");
    }

    private void putWithTime(String string)
    {
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        string = LocalDateTime.now().format(FORMATTER) + " " + string;
        try
        {
            queue.put(string);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Failed to log message, due to: " + e);
        }
    }

    private void put(String string)
    {
        try
        {
            queue.put(string);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Failed to log message, due to: " + e);
        }
    }

    public void infoInline(String string)
    {
        if (!level.equals(Level.ERROR))
        {
            put(string);
        }
    }

    public void infoBreakLine()
    {
        if (!level.equals(Level.ERROR))
        {
            put("\n");
        }
    }

    public void debugInline(String string)
    {
        if (level.equals(Level.DEBUG))
        {
            put(string);
        }
    }

    public void debugBreakLine()
    {
        if (level.equals(Level.DEBUG))
        {
            putWithTime("\n");
        }
    }

    public void errorInline(String string)
    {
        putWithTime(string);
    }


    // =================================================================================================================

    public void start()
    {
        thread.start();
        run = true;
    }

    public void stop()
    {
        run = false;
        thread.interrupt();
    }


    // =================================================================================================================

    @Override
    public void run()
    {
        while (run)
        {
            try
            {
                System.out.print(queue.take());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
