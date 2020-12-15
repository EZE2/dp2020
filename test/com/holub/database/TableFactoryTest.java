package com.holub.database;

import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import static org.junit.Assert.*;

public class TableFactoryTest {

    // XMLImporter 테스트
    @Test
    public void load() throws IOException {
        Reader in = new FileReader( new File( "C:/dp2020/Dbase", "address.xml"));
        ConcreteTable loaded = new ConcreteTable( new XMLImporter( in ));

        assertEquals(5, loaded.rows().columnCount());
        assertEquals("address", loaded.rows().tableName());
//        assertEquals("wrong", loaded.rows().tableName());
        System.out.println(loaded.toString());
        in.close();
    }
}