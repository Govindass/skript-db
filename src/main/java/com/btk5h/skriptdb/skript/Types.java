package com.btk5h.skriptdb.skript;

import com.zaxxer.hikari.HikariDataSource;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;

public class Types {
  static {
    Classes.registerClass(new ClassInfo<>(HikariDataSource.class, "datasource")
        .user("datasources?")
        .parser(new Parser<HikariDataSource>() {
          @Override
          public HikariDataSource parse(String s, ParseContext context) {
            return null;
          }

          @Override
          public String toString(HikariDataSource o, int flags) {
            return o.getJdbcUrl();
          }

          @Override
          public String toVariableNameString(HikariDataSource o) {
            return o.getJdbcUrl();
          }

          @Override
          public String getVariableNamePattern() {
            return "jdbc:.+";
          }
        })
        .serializer(new Serializer<HikariDataSource>() {
          @Override
          public Fields serialize(HikariDataSource o) throws NotSerializableException {
            Fields fields = new Fields();
            fields.putObject("jdbcurl", o.getJdbcUrl());
            return fields;
          }

          @Override
          public void deserialize(HikariDataSource o, Fields f) throws StreamCorruptedException,
              NotSerializableException {
          }

          @Override
          protected HikariDataSource deserialize(Fields fields) throws StreamCorruptedException,
              NotSerializableException {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl((String) fields.getObject("jdbcurl"));
            return ds;
          }

          @Override
          public boolean mustSyncDeserialization() {
            return false;
          }

          @Override
          protected boolean canBeInstantiated() {
            return false;
          }
        }));
  }
}
