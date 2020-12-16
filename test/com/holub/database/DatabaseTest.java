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
    // 1번 기능 테스트
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

        // 출력 결과는 Dbase/address.html에 저장됨.
        theDatabase.HTMLdump();
        Table t = theDatabase.execute("SELECT * FROM address");
        // 열이 5개가 맞는지 테스트
        assertEquals(5,t.rows().columnCount());
        System.out.println(t.toString());
    }

    // 3번 요구사항 테스트
    @Test
    public void doselect() throws Exception {
        Database db = new Database("C:/dp2020");
        String testSQL = "select * from address, name where address.addrId = name.addrId";

        Table result = db.execute(testSQL);

        assertEquals(7, result.rows().columnCount());
        System.out.println(result.toString());
        
        // 기존에 되던 여러 테이블 SELECT문도 테스트
        String  testSQL2 = "select street from address, name where address.addrId = name.addrId";
        Table result2 = db.execute(testSQL2);
        assertEquals(1, result2.rows().columnCount());
        System.out.println(result2.toString());
        

    }

    // 2번 기능 테스트 - Export
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
        // 출력 결과는 Dbase/address.xml에 저장됨.
        theDatabase.XMLdump();
        Table t = theDatabase.execute("SELECT * FROM address");
        // 열이 5개가 맞는지 테스트
        assertEquals(5,t.rows().columnCount());
        System.out.println(t.toString());
    }
}
