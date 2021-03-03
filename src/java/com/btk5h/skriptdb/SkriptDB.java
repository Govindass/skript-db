/*
 * MIT License
 *
 * Copyright (c) 2016 Bryan Terce
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.btk5h.skriptdb;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;

/**
 * # skript-db
 *
 * > Sensible SQL support for Skript.
 *
 * @index -1
 */
public final class SkriptDB extends JavaPlugin {

  private static SkriptDB instance;
  private static SkriptAddon addonInstance;

  private static RowSetFactory rowSetFactory;
  protected FileConfiguration config;

  public SkriptDB() {
    if (instance == null) {
      instance = this;
    } else {
      throw new IllegalStateException();
    }
  }

  private void setupConfig() throws IOException {
    //don't check if it exists, because mkdir already does that
    File file = new File("plugins/skript-db/config.yml");
    if (file.getParentFile().mkdirs()) {
      BufferedWriter out = null;
      try {
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("plugins/skript-db/config.yml", false), StandardCharsets.UTF_8));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      try {
        if (out == null) return;
        out.write("# Only change this if you wish to use a different driver than Java's default, like MariaDB driver.\n");
        out.write("# If you use MariaDB, its driver is shaded together with skript-db, so you can just specify:" + "\"org.mariadb.jdbc.Driver\"" + ".\n");
        out.write("sql-driver-class-name: " + "\"default\"" + "\n");
      } catch (IOException e) {
        e.printStackTrace();
      }

      try {
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onEnable() {
    try {
      setupConfig();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        rowSetFactory = RowSetProvider.newFactory();

        getAddonInstance().loadClasses("com.btk5h.skriptdb.skript");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static SkriptAddon getAddonInstance() {
    if (addonInstance == null) {
      addonInstance = Skript.registerAddon(getInstance());
    }
    return addonInstance;
  }

  public static SkriptDB getInstance() {
    if (instance == null) {
      throw new IllegalStateException();
    }
    return instance;
  }

  public static RowSetFactory getRowSetFactory() {
    return rowSetFactory;
  }
}
