package com.holub.database;

import com.holub.text.ParseFailure;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

public class DatabaseTest {
    @Test
    public void HTMLdump() throws IOException, ParseFailure {
        Database theDatabase = new Database();

        // Read a sequence of SQL statements in from the file
        // Database.test.sql and execute them.

        BufferedReader sql = new BufferedReader(
                new FileReader("SimpleTest.sql"));
        String test;
        while ((test = sql.readLine()) != null) {
            test = test.trim();
            if (test.length() == 0)
                continue;

            while (test.endsWith("\\")) {
                test = test.substring(0, test.length() - 1);
                test += sql.readLine().trim();
            }

            System.out.println("Parsing: " + test);
            Table result = theDatabase.execute(test);

            if (result != null)    // it was a SELECT of some sort
                System.out.println(result.toString());
        }

        theDatabase.HTMLdump();
        System.out.println("Database PASSED");
        System.exit(0);
    }

    @Test
    public void doselect() throws Exception {
        Database db = new Database("C:/dp2020");
        String testSQL = "select * from address, name where address.addrId = name.addrId";

        Table result = db.execute(testSQL);
        System.out.println(result.toString());

    }

    @Test
    public void XMLdump() throws IOException, ParseFailure {
        Database theDatabase = new Database();

        // Read a sequence of SQL statements in from the file
        // Database.test.sql and execute them.

        BufferedReader sql = new BufferedReader(
                new FileReader("SimpleTest.sql"));
        String test;
        while ((test = sql.readLine()) != null) {
            test = test.trim();
            if (test.length() == 0)
                continue;

            while (test.endsWith("\\")) {
                test = test.substring(0, test.length() - 1);
                test += sql.readLine().trim();
            }

            System.out.println("Parsing: " + test);
            Table result = theDatabase.execute(test);

            if (result != null)    // it was a SELECT of some sort
                System.out.println(result.toString());
        }

        theDatabase.XMLdump();
        System.out.println("Database PASSED");
        System.exit(0);
    }
}
