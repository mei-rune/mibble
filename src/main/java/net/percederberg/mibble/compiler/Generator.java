package net.percederberg.mibble.compiler;

import net.percederberg.mibble.MibTypeSymbol;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpTextualConvention;

import java.io.IOException;

public interface Generator {
    void GenerateGoType(MibTypeSymbol mibSymbol, SnmpTextualConvention symbol) throws IOException;
    void GenerateGoArray(MibValueSymbol valueSymbol, MibValueSymbol[] children) throws IOException;
    void GenerateGoObject(MibValueSymbol valueSymbol, MibValueSymbol[] children) throws IOException;
    void Close() throws IOException;
}

