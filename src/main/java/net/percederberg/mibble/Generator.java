package net.percederberg.mibble;

import net.percederberg.mibble.snmp.SnmpIndex;
import net.percederberg.mibble.snmp.SnmpObjectType;
import net.percederberg.mibble.snmp.SnmpTextualConvention;
import net.percederberg.mibble.snmp.SnmpType;
import net.percederberg.mibble.type.*;
import net.percederberg.mibble.value.ObjectIdentifierValue;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface Generator {
    void GenerateMeta(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException;
    void GenerateGo(MibValueSymbol symbol, MibValueSymbol[]  elementTypes) throws IOException;
    void GenerateGoType(MibTypeSymbol mibSymbol, SnmpTextualConvention symbol) throws IOException;
    void Close() throws IOException;
}

class GeneratorImpl implements Generator {
    Writer metaWriter;
    Writer srcWriter;
    Map<String,String> tables = new HashMap<>();
    String managedObject;
    String module;

    public GeneratorImpl(String managedObject, String module, Writer meta, Writer src) throws IOException {
        this.managedObject = managedObject;
        this.module = module;
        this.metaWriter = meta;
        this.srcWriter = src;
        this.srcWriter.append("package metrics\r\n\r\n");
    }

    @Override
    public void GenerateMeta(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException {
        metaWriter.append(String.format("  <metric name=\"%s\" is_array=\"true\">\n", symbol.getParent().getName()));
        String classComment = ((SnmpType)symbol.getParent().getType()).getDescription();
        if(null != classComment && !classComment.trim().isEmpty()) {
            metaWriter.append(String.format("    <label lang=\"zh-cn\">%s</label>\n", classComment));
        }
        metaWriter.append(String.format("    <class name=\"%s\">\r\n", symbol.getParent().getName()));
        for(MibValueSymbol el : elementTypes) {
            String comment = ((SnmpType)el.getType()).getDescription();
            if(null != comment && comment.trim().isEmpty()) {
                comment = null;
            }

            TypeValue metaType = toMetaType(el.getType());
            StringValue[] values = getEnum(el.getType());
            if(null == comment && (0 == values.length)) {
                metaWriter.append(String.format("      <property name=\"%s\" type=\"%s\" />%s\r\n",
                        el.getName(), metaType.name, metaType.GetXmlComment()));
            } else {
                metaWriter.append(String.format("      <property name=\"%s\" type=\"%s\">\r\n",
                        el.getName(), metaType.name));
                if(!metaType.GetXmlComment().isEmpty()) {
                    metaWriter.append(String.format("        %s\n", metaType.GetXmlComment()));
                }
                if(null != comment && !comment.trim().isEmpty()) {
                    metaWriter.append(String.format("        <label lang=\"zh-cn\">%s</label>\n", comment));
                }
                if(0 != values.length) {
                    metaWriter.append("          <enumeration>\r\n");
                    for(StringValue v : values) {
                        metaWriter.append(String.format("            <value name=\"%s\">%s</value>\r\n", v.name, v.value));
                    }
                    metaWriter.append("          </enumeration>\r\n");
                }
                metaWriter.append("      </property>\r\n");
            }
        }
        metaWriter.append("    </class>\r\n");
        metaWriter.append("  </metric>\n");
        metaWriter.flush();
    }

    private StringValue[] getEnum(MibType type) {
        if(type instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) type;
            if( objectType.getSyntax() instanceof IntegerType) {
                IntegerType integerType = (IntegerType)objectType.getSyntax();
                MibValueSymbol[] symbols = integerType.getAllSymbols();
                if(null != symbols && symbols.length > 0 ) {
                    StringValue[] results = new StringValue[symbols.length];
                    for (int i =0; i < symbols.length; i ++ ) {
                        results[i] = new StringValue();
                        results[i].name = symbols[i].getName();
                        results[i].value = symbols[i].getValue().toString();
                    }
                    return results;
                }
            }
        }
        return new StringValue[0];
    }

    private TypeValue toMetaType(MibType type) {
        if(type instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) type;
            if( objectType.getSyntax() instanceof IntegerType) {
                IntegerType integerType = (IntegerType) objectType.getSyntax();
                if(null != integerType) {
                    MibTypeSymbol symbol = integerType.getReferenceSymbol();
                    if(null != symbol) {
                        String comment = symbol.getName();
                        do {
                            if ("Counter".equalsIgnoreCase(symbol.getName())) {
                                return new TypeValue("biginteger", comment);
                            }
                            if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                                return new TypeValue("biginteger", comment);
                            }
                            if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                                return new TypeValue("biginteger", comment);
                            }
                            if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                                return new TypeValue("biginteger", comment);
                            }
                            if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                                return new TypeValue("biginteger", comment);
                            }
                            if(symbol.getType() instanceof SnmpTextualConvention) {
                                symbol = ((SnmpTextualConvention) symbol.getType()).getSyntax().getReferenceSymbol();
                            } else {
                                break;
                            }
                        } while (null != symbol);
                        return new TypeValue("integer", comment);
                    }
                }
                return new TypeValue("integer", null);
            } else if( objectType.getSyntax() instanceof StringType) {
                return new TypeValue("string", null);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                return new TypeValue("string", null);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                return new TypeValue("string", null);
            } else if( objectType.getSyntax() instanceof RealType) {
                return new TypeValue("decimal", null);
            } else if( objectType.getSyntax() instanceof BooleanType) {
                return new TypeValue("boolean", null);
            }

        }
        throw new RuntimeException(type.toString() + "is unsupported.");
    }

    @Override
    public void GenerateGo(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException {
        srcWriter.append(String.format("type %s struct {\r\n  snmpBase\r\n}\r\n\n", symbol.getName()));
        srcWriter.append(String.format("func (self *%s) Call(params sampling.MContext) sampling.Result {\r\n", symbol.getName()));

        MibValue arguments = ((SnmpObjectType) symbol.getType()).getAugments();
        if(null != arguments ) {
            ObjectIdentifierValue args = (ObjectIdentifierValue) arguments;
            generateReadByArguments(symbol, elementTypes, args);
        } else {
            generateReadAll(symbol, elementTypes);
        }
        srcWriter.append("}\r\n\r\n");
        srcWriter.flush();

        tables.put(symbol.getName(), symbol.getName());
    }

    private void generateReadByArguments(MibValueSymbol symbol, MibValueSymbol[] elementTypes, ObjectIdentifierValue args) throws IOException {
        srcWriter.append("  oids = []string{");
        for(MibValueSymbol el : elementTypes) {
            if (((SnmpObjectType)el.getType()).getAccess().canRead()) {
                srcWriter.append("    \"");
                srcWriter.append(((ObjectIdentifierValue) el.getValue()).toString());
                srcWriter.append(".\" + key,\r\n");
            }
        }
        srcWriter.append("  }\r\n");
        srcWriter.append("  values, e := self.Get(params, oids)\r\n");
        srcWriter.append("  if nil != e {\r\n");
        srcWriter.append("    return self.Return(nil, e)\r\n");
        srcWriter.append("  }\r\n");
        srcWriter.append("  return self.OK(map[string]interface{}{\"key\":                   key,\r\n");

        int i = 0;
        for (MibValueSymbol el : elementTypes) {
            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                srcWriter.append(String.format("    //%s can't read.\r\n", el.getName()));
                continue;
            }

            srcWriter.append(String.format("    \"%s\":         %s,\r\n",
                    el.getName(), toGoMethod(el, String.format("oids[%d]", i))));
            i ++;
        }
        srcWriter.append("  })\r\n");
    }

    private void generateReadAll(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException {
        StringBuilder columns = new StringBuilder();
        for(MibValueSymbol el : elementTypes) {
            if (((SnmpObjectType)el.getType()).getAccess().canRead()) {
                columns.append(((ObjectIdentifierValue) el.getValue()).getValue());
                columns.append(", ");
            }
        }

        if(columns.length() >= 2) {
            columns.setLength(columns.length() - 2);
        }


        srcWriter.append(String.format("  return self.GetAllResult(params, \"%s\", []int{%s},\r\n", ((ObjectIdentifierValue) symbol.getValue()).toString(), columns));
        srcWriter.append("    func(key string, old_row map[string]interface{}) (interface{}, error) {\r\n");

        ArrayList<SnmpIndex> indexes = ((SnmpObjectType) symbol.getType()).getIndex();
        ArrayList<MibValueSymbol> indexSymbols = new ArrayList<>();
        if (null != indexes && !indexes.isEmpty()) {
            for (SnmpIndex index : indexes) {
                indexSymbols.add(((ObjectIdentifierValue) index.getValue()).getSymbol());
            }
            if (indexes.size() == 1) {
                srcWriter.append("      ").append(indexSymbols.get(0).getName()).append(" = key\r\n");
            } else {
                boolean is_first = true;
                for (MibValueSymbol indexSym : indexSymbols) {
                    if(is_first) {
                        srcWriter.append("      ").append(indexSym.getName()).
                                append(", next, e := ").append(toGoReadMethod(indexSym, "key")).append("\r\n");
                        is_first = false;
                    } else {
                        srcWriter.append("      ").append(indexSym.getName()).
                                append(", next, e := ").append(toGoReadMethod(indexSym, "next")).append("\r\n");
                    }

                    srcWriter.append("      if nil != e {\r\n")
                            .append("        return self.Return(nil, errors.New(\"failed to read ")
                            .append(indexSym.getName())
                            .append(", key '\" + key + \"' is invalid.\"))\r\n")
                            .append("      }\r\n");
                }
            }
        }
        srcWriter.append("      return map[string]interface{}{\"key\":                   key,\r\n");


        for (MibValueSymbol el : elementTypes) {
            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                boolean found = false;
                for (MibValueSymbol indexSym : indexSymbols) {
                    if (indexSym.equals(el)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    srcWriter.append(String.format("        //%s can't read.\r\n", el.getName()));
                    continue;
                }

                srcWriter.append(String.format("        \"%s\":         %s,\r\n", el.getName(), el.getName()));
                continue;
            }
            srcWriter.append(String.format("        \"%s\":         %s,\r\n", el.getName(), toGoMethod(el)));
        }
        srcWriter.append("    }, nil\r\n");
        srcWriter.append("  })\r\n");
    }

    @Override
    public void GenerateGoType(MibTypeSymbol symbol, SnmpTextualConvention type) throws IOException {
        if(null != type.getDescription() && !type.getDescription().trim().isEmpty()) {
            String[] ss = type.getDescription().trim().split("\n");
            for (String s : ss) {
                srcWriter.append("// ").append(s.trim()).append("\r\n");
            }
        }
        srcWriter.append("func SnmpGet").append(symbol.getName());
        GoStringValue value = toGoType(type);
        srcWriter.append(String.format("(params sampling.MContext, values map[string]interface{}, idx string) (%s, error) {\r\n", value.name));
        srcWriter.append(String.format("  return SnmpGet%s(params, values, idx, %s)\r\n", value.methodName, value.value));
        srcWriter.append("}\r\n");
        srcWriter.append("\r\n");
        srcWriter.append("\r\n");
    }

    private GoStringValue toGoType(SnmpTextualConvention type) {
        if( type.getSyntax() instanceof IntegerType) {
            return new GoStringValue("Int", "int", "0");
        } else if( type.getSyntax() instanceof StringType) {
            return new GoStringValue("String", "string", "\"\"");
        } else if( type.getSyntax() instanceof BitSetType) {
            return new GoStringValue("Bits", "string", "\"\"");
        } else if( type.getSyntax() instanceof ObjectIdentifierType) {
            return new GoStringValue("Oid", "string", "\"\"");
        }
        throw new RuntimeException(type.toString() + "is unsupported.");
    }

    private String toGoMethod(MibValueSymbol el) {
        return toGoMethod(el, "\"" + Integer.toString(((ObjectIdentifierValue) el.getValue()).getValue())+ "\"");
    }

    @SuppressWarnings("Duplicates")
    private String toGoMethod(MibValueSymbol el, String oid) {
        if(el.getType() instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) el.getType();
            if( objectType.getSyntax() instanceof IntegerType) {
                MibTypeSymbol symbol = objectType.getSyntax().getReferenceSymbol();
                if(null != symbol) {
                    if ("Counter".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetCounter32With(params, old_row, %s, 0)", oid);
                    }
                    if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetUnsigned32With(params, old_row, %s, 0)", oid);
                    }
                    if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetCounter32With(params, old_row, %s, 0)", oid);
                    }
                    if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetCounter64With(params, old_row, %s, 0)", oid);
                    }
                    if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetGauge32With(params, old_row, %s, 0)", oid);
                    }
                    if ("GAUGE".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpGetGauge32With(params, old_row, %s, 0)", oid);
                    }
                    return String.format("SnmpGet%s(params, old_row, %s)", symbol.getName(), oid);
                }
                return String.format("SnmpGetIntWith(params, old_row, %s, 0)", oid);
            } else if( objectType.getSyntax() instanceof StringType) {
                    return String.format("SnmpGetStringWith(params, old_row, %s, \"\")", oid);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                    return String.format("SnmpGetStringWith(params, old_row, %s, \"\")", oid);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                    return String.format("SnmpGetOidWith(params, old_row, %s, \"\")", oid);
            }
        }
        throw new RuntimeException(el.getType().toString() + "is unsupported.");
    }


    @SuppressWarnings("Duplicates")
    private String toGoReadMethod(MibValueSymbol el, String varName) {
        if(el.getType() instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) el.getType();
            if( objectType.getSyntax() instanceof IntegerType) {
                MibTypeSymbol symbol = objectType.getSyntax().getReferenceSymbol();
                if(null != symbol) {
                    if ("Counter".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadtCounter32FromOid(params, old_row, %s)", varName);
                    }
                    if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadUnsigned32FromOid(params, old_row, %s)", varName);
                    }
                    if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadCounter32FromOid(params, old_row, %s)", varName);
                    }
                    if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadCounter64FromOid(params, old_row, %s)", varName);
                    }
                    if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadGauge32FromOid(params, old_row, %s)", varName);
                    }
                    if ("GAUGE".equalsIgnoreCase(symbol.getName())) {
                        return String.format("SnmpReadGauge32FromOid(params, old_row, %s)", varName);
                    }
                    return String.format("SnmpRead%sFromOid(params, old_row, %s)", symbol.getName(), varName);
                }
                return String.format("SnmpReadIntFromOid(params, old_row, %s)", varName);
            } else if( objectType.getSyntax() instanceof StringType) {
                return String.format("SnmpReadStringFromOid(params, old_row, %s)", varName);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                return String.format("SnmpReadBitsFromOid(params, old_row,  %s)", varName);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                return String.format("SnmpReadOidFromOid(params, old_row, %s)", varName);
            }
        }
        throw new RuntimeException(el.getType().toString() + "is unsupported.");
    }

    @Override
    public void Close() throws IOException {
        if(!tables.isEmpty()) {
            srcWriter.append("func init() {\r\n");
            for(Map.Entry<String, String> entry : tables.entrySet()) {
                srcWriter.append(" sampling.RegisterRouteSpec(\"").append(entry.getKey()).append("_default\", \"get\", \"")
                        .append(this.managedObject).append("\", \"").append(entry.getKey()).append("\", \"\", nil,\r\n")
                    .append("    func(rs *sampling.RouteSpec, params map[string]interface{}) (sampling.Method, error) {\r\n")
                    .append("      drv := &").append(entry.getKey()).append("{}\r\n")
                    .append("      return drv, drv.Init(rs, params)\r\n")
                    .append("    })\r\n");
            }
            srcWriter.append("}\r\n\r\n");


            String moduleName = toGoName(module);
            srcWriter.append("var ").append(moduleName).append(" = []MibModule{\r\n");
            for(Map.Entry<String, String> entry : tables.entrySet()) {
                srcWriter.append("  {Name: \"").append(entry.getKey()).append("\", IsArray: true},\r\n");
            }
            srcWriter.append("}\r\n");
        }

        metaWriter.close();
        srcWriter.close();
    }

    private String toGoName(String module) {
        return module.replaceAll(" ", "_")
                .replaceAll("\t", "_")
                .replaceAll("\r", "_")
                .replaceAll("\n", "_")
                .replaceAll("-", "_")
                .replaceAll(",", "_")
                .replaceAll(":", "_")
                .replaceAll(";", "_");
    }

    public static class StringValue {
        public String name;
        public String value;

        public StringValue() {
        }

        public StringValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class GoStringValue {
        public String methodName;
        public String name;
        public String value;

        public GoStringValue(String methodName, String name, String value) {
            this.methodName = methodName;
            this.name = name;
            this.value = value;
        }
    }

    public static class TypeValue {
        public String name;
        public String comment;

        public TypeValue(String name, String comment) {
            this.name = name;
            this.comment = comment;
        }

        public String GetXmlComment() {
            if(null == this.comment) {
                return "";
            }
            if(this.comment.trim().isEmpty()) {
                return "";
            }

            return "<!-- "+this.comment.trim()+" -->";
        }
    }
}