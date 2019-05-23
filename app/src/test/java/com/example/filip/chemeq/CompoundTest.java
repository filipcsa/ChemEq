package com.example.filip.chemeq;

import com.example.filip.chemeq.model.Compound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompoundTest {

    @Test
    public void copyCompoundTest() {
        Compound a = new Compound();
        a.setCompound("Hello");
        Compound b = a.getCopy();
        b.addCharacter(" world");

        assertEquals("Hello world", b.getCompound());
        assertEquals("Hello", a.getCompound());
    }

    @Test
    public void getLastCharacterTest() {
        Compound a = new Compound();
        a.setCompound("NaL");
        assertEquals("L", a.getLastCharacter());

        Compound b = new Compound();
        assertEquals("", b.getLastCharacter());

        b.addCharacter("L");
        assertEquals("L", b.getLastCharacter());
    }
}
