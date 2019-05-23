package com.example.filip.chemeq;

import org.junit.Test;

import java.math.BigInteger;

public class Vycisleni {

    @Test
    public void bigIntTestGCD() {
        BigInteger num = BigInteger.valueOf(75);
        BigInteger denom = BigInteger.valueOf(100);
        BigInteger _gcd = num.gcd(denom);

        System.out.println(_gcd);

        double d = 1/2f;
        System.out.println(String.valueOf(d));
    }
}
