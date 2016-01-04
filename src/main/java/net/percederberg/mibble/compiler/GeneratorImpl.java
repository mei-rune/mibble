package net.percederberg.mibble.compiler;

import net.percederberg.mibble.MibType;
import net.percederberg.mibble.MibTypeSymbol;
import net.percederberg.mibble.MibValue;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpIndex;
import net.percederberg.mibble.snmp.SnmpObjectType;
import net.percederberg.mibble.snmp.SnmpTextualConvention;
import net.percederberg.mibble.snmp.SnmpType;
import net.percederberg.mibble.type.*;
import net.percederberg.mibble.value.ObjectIdentifierValue;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2015/11/30.
 */
class GeneratorImpl implements Generator {
    Writer metaWriter;
    Writer srcWriter;
    Map<String,MetricSpec> tables = new HashMap<>();
    String managedObject;
    String module;
    Map<String, MibValueSymbol> groups = new HashMap<>();
    boolean is_only_types;


    public GeneratorImpl(String namespace, String managedObject, String module, Writer meta, Writer src, boolean is_only_types) throws IOException {
        this.managedObject = managedObject;
        this.module = module;
        this.metaWriter = meta;
        this.srcWriter = src;
        this.is_only_types = is_only_types;
        this.srcWriter.append("// 这是代码生成的文件，请不要修改它\r\n")
                .append("package ").append(namespace).append("\r\n\r\n"+
        "import (\r\n" +
                "\t\"cn/com/hengwei/sampling\"\r\n" +
                "\t. \"cn/com/hengwei/sampling/drivers/snmp\"\r\n" +
                "\t\"cn/com/hengwei/sampling/metrics\"\r\n"+
                "\t\"cn/com/hengwei/sampling/metrics\"\r\n" +
                "\t\"errors\"\r\n" +
                ")\r\n\r\n");

        if(null != metaWriter) {
            this.metaWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n")
                    .append("<!-- 这是代码生成的文件，请不要修改它 -->\r\n")
                    .append("<metricDefinitions lastModified=\"")
                    .append(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(new Date()))
                    .append("\"")
                    .append(" class=\"").append(managedObject).append("\"\r\n")
                    .append("   xmlns=\"http://schemas.hengwei.com.cn/tpt/1/metricDefinitions\"")
                    .append("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                    .append("   xsi:schemaLocation=\"http://schemas.hengwei.com.cn/tpt/1/metricDefinitions metricDefinitions.xsd\">\r\n");
        }
    }

    public void GenerateMetaTable(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException {
        if(null == metaWriter) {
            return;
        }

        MibValue arguments = ((SnmpObjectType) symbol.getType()).getAugments();
        if (null != arguments) {
            metaWriter.append(String.format("  <metric name=\"%s\">\r\n", symbol.getParent().getName()));
        } else {
            metaWriter.append(String.format("  <metric name=\"%s\" is_array=\"true\">\r\n", symbol.getParent().getName()));
        }
        String classComment = ((SnmpType)symbol.getParent().getType()).getDescription();
        if(null != classComment && !classComment.trim().isEmpty()) {
            metaWriter.append(String.format("    <description lang=\"zh-cn\">%s</description>\r\n", escapeXml(classComment)));
        }
        metaWriter.append(String.format("    <class name=\"%s\">\r\n", symbol.getParent().getName()));
        metaWriter.append("      <property name=\"key\" type=\"string\">\r\n")
                .append("        <label lang=\"zh-cn\">索引</label>\r\n")
                .append("      </property>\r\n");

        generateChildrenMeta(elementTypes);
        metaWriter.append("    </class>\r\n");

        if(null != arguments ) {
            metaWriter.append("    <arguments>\r\n" +
                    "      <argument name=\"key\" type=\"string\">\r\n" +
                    "        <label lang=\"zh-cn\">索引</label>\r\n" +
                    "        <required/>\r\n" +
                    "      </argument>\r\n" +
                    "    </arguments>\r\n");
        }
        metaWriter.append("  </metric>\r\n");
        metaWriter.flush();
    }

    private String escapeXml(String txt) {
        return Entities.XML.escape(txt);
    }

    private void GenerateMetaObject(MibValueSymbol symbol, MibValueSymbol[] children) throws IOException {
        if(null == metaWriter) {
            return;
        }

        children = toLeafOnly(children);
        if(0 == children.length) {
            return;
        }

        metaWriter.append(String.format("  <metric name=\"%s\">\r\n", symbol.getName()));
//        String classComment = ((SnmpType)symbol.getType()).getDescription();
//        if(null != classComment && !classComment.trim().isEmpty()) {
//            metaWriter.append(String.format("    <description lang=\"zh-cn\">%s</description>\n", classComment));
//        }
        metaWriter.append(String.format("    <class name=\"%s\">\r\n", symbol.getName()));
        generateChildrenMeta(children);
        metaWriter.append("    </class>\r\n");
        metaWriter.append("  </metric>\r\n");
        metaWriter.flush();
    }

    private void generateChildrenMeta(MibValueSymbol[] children) throws IOException {
        for(MibValueSymbol el : children) {
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
                    metaWriter.append(String.format("        <description lang=\"zh-cn\">%s</description>\n", escapeXml(comment)));
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

    public void GenerateGoTable(MibValueSymbol symbol, MibValueSymbol[] elementTypes) throws IOException {
        String name = symbol.getParent().getName();
        srcWriter.append(String.format("type %s struct {\r\n  metrics.SnmpBase\r\n}\r\n\n", name));
        srcWriter.append(String.format("func (self *%s) Call(params sampling.MContext) sampling.Result {\r\n", name));

        MibValue arguments = ((SnmpObjectType) symbol.getType()).getAugments();
        if(null != arguments ) {
            ObjectIdentifierValue args = (ObjectIdentifierValue) arguments;
            generateReadByArguments(symbol, elementTypes, args);

            tables.put(name, new MetricSpec(name, name, false));
        } else {
            generateReadAll(symbol, elementTypes);
            tables.put(name, new MetricSpec(name, name, true));
        }
        srcWriter.append("}\r\n\r\n");
        srcWriter.flush();
    }

    private void generateReadByArguments(MibValueSymbol symbol, MibValueSymbol[] elementTypes, ObjectIdentifierValue args) throws IOException {
        srcWriter.append("  key := params.GetStringWithDefault(\"key\", \"\")\r\n")
                .append("  if \"\" == key {\r\n")
                .append("    return self.Return(nil, sampling.BadRequest(\"'key' is missing.\"))\r\n")
                .append("  }\r\n\r\n");
        generateReadByIdx(symbol, elementTypes, "key");
    }

    private void generateReadByIdx(MibValueSymbol symbol, MibValueSymbol[] elementTypes, String keyName) throws IOException {
            srcWriter.append("  oids := []string{");
        for(MibValueSymbol el : elementTypes) {
            if (((SnmpObjectType)el.getType()).getAccess().canRead()) {
                srcWriter.append("    \"");
                srcWriter.append(el.getValue().toString());
                srcWriter.append(".\" + ").append(keyName).append(",\r\n");
            }
        }
        srcWriter.append("  }\r\n");
        srcWriter.append("  values, e := self.Get(params, oids)\r\n");
        srcWriter.append("  if nil != e {\r\n");
        srcWriter.append("    return self.Return(nil, e)\r\n");
        srcWriter.append("  }\r\n");


        int i = 0;
        for (MibValueSymbol el : elementTypes) {
            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                srcWriter.append(String.format("    //%s can't read.\r\n", el.getName()));
                continue;
            }

            if(isPreRead(el)) {
                srcWriter.append(String.format("  %s := %s\r\n",
                        el.getName(), toGoMethod(el, null,   "values", String.format("oids[%d]", i))));
            }
            i ++;
        }

        srcWriter.append("  return self.OK(map[string]interface{}{\"key\":          ").append(keyName).append(",\r\n");

        i = 0;
        MibValueSymbol prev_el = null;
        for (MibValueSymbol el : elementTypes) {
            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                srcWriter.append(String.format("    //%s can't read.\r\n", el.getName()));
                continue;
            }

            if(isPreRead(el)) {
                srcWriter.append(String.format("    \"%s\":         %s,\r\n",el.getName(), el.getName()));
                prev_el = el;
            } else {
                srcWriter.append(String.format("    \"%s\":         %s,\r\n",
                        el.getName(), toGoMethod(el, prev_el, "values", String.format("oids[%d]", i))));
            }
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

        srcWriter.append("  oid_idx := params.GetStringWithDefault(\"key\", \"\")\r\n");
        srcWriter.append("  if \"\" != oid_idx {\r\n");
        generateReadByIdx(symbol, elementTypes, "oid_idx");
        srcWriter.append("  }\r\n");


        srcWriter.append(String.format("  return self.GetAllResult(params, \"%s\", []int{%s},\r\n", symbol.getValue().toString(), columns));
        srcWriter.append("    func(key string, old_row map[string]interface{}) (interface{}, error) {\r\n");

        ArrayList<SnmpIndex> indexes = ((SnmpObjectType) symbol.getType()).getIndex();
        ArrayList<MibValueSymbol> indexSymbols = new ArrayList<>();
        if (null != indexes && !indexes.isEmpty()) {
            for (SnmpIndex index : indexes) {
                indexSymbols.add(((ObjectIdentifierValue) index.getValue()).getSymbol());
            }
            if (indexes.size() == 1) {
                MibValueSymbol idxSymbol = indexSymbols.get(0);
                if( idxSymbol.getType() instanceof SnmpObjectType &&
                ((SnmpObjectType) idxSymbol.getType()).getSyntax() instanceof StringType) {
                    srcWriter.append("      ").append(idxSymbol.getName()).
                            append(", _, e := ").append(toGoReadMethod(idxSymbol, null, "ToOidFromString(key)")).append("\r\n");

                    srcWriter.append("      if nil != e {\r\n")
                            .append("        return nil, errors.New(\"failed to read ")
                            .append(idxSymbol.getName())
                            .append(", key '\" + key + \"' is invalid.\")\r\n")
                            .append("      }\r\n");
                } else {
                    srcWriter.append("      ").append(idxSymbol.getName()).append(" := key\r\n");
                }
            } else {
                boolean is_first = true;
                MibValueSymbol prev_el = null;
                for (MibValueSymbol indexSym : indexSymbols) {
                    if(is_first) {
                        srcWriter.append("      ").append(indexSym.getName()).
                                append(", next, e := ").append(toGoReadMethod(indexSym, null, "ToOidFromString(key)")).append("\r\n");
                        is_first = false;
                    } else {
                        if(isPreRead(indexSym)) {
                            prev_el = indexSym;
                        }
                        srcWriter.append("      ").append(indexSym.getName()).
                                append(", next, e := ").append(toGoReadMethod(indexSym, prev_el, "next")).append("\r\n");
                    }

                    srcWriter.append("      if nil != e {\r\n")
                            .append("        return nil, errors.New(\"failed to read ")
                            .append(indexSym.getName())
                            .append(", key '\" + key + \"' is invalid.\")\r\n")
                            .append("      }\r\n");
                }
            }
        }

        for (MibValueSymbol el : elementTypes) {
            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                continue;
            }
            if(isPreRead(el)) {
                srcWriter.append(String.format("      %s := %s\r\n", el.getName(), toGoMethod(el, null)));
            }
        }
        srcWriter.append("      return map[string]interface{}{\"key\":                   key,\r\n");



        for (MibValueSymbol indexSym : indexSymbols) {
            srcWriter.append(String.format("        \"%s\":         %s,\r\n", indexSym.getName(), indexSym.getName()));
        }
        MibValueSymbol prev_el = null;
        for (MibValueSymbol el : elementTypes) {
            boolean found = false;
            for (MibValueSymbol indexSym : indexSymbols) {
                if (indexSym.equals(el)) {
                    found = true;
                    break;
                }
            }

            if (!((SnmpObjectType) el.getType()).getAccess().canRead()) {
                if (!found) {
                    srcWriter.append(String.format("        //%s can't read.\r\n", el.getName()));
                    continue;
                }

                //srcWriter.append(String.format("        \"%s\":         %s,\r\n", el.getName(), el.getName()));
                continue;
            }

            if (found) {
                continue;
            }
            if(isPreRead(el)) {
                srcWriter.append(String.format("        \"%s\":         %s,\r\n", el.getName(), el.getName()));
                prev_el = el;
            } else {
                srcWriter.append(String.format("        \"%s\":         %s,\r\n", el.getName(), toGoMethod(el, prev_el)));
            }
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
        srcWriter.append(String.format("(params sampling.MContext, values map[string]interface{}, idx string) %s {\r\n", value.name));
        if(null != value.size && !value.size.isEmpty()) {
            srcWriter.append(String.format("  return SnmpGetFixed%sWith(params, values, idx, %s, \"%s\", %s)\r\n",
                    value.methodName, value.size, value.displayHint, value.value));
        } else if(null != value.displayHint && value.displayHint.isEmpty()) {
            srcWriter.append(String.format("  return SnmpGet%sWithDisplayHintAndDefaultValue(params, values, idx,  \"%s\", %s)\r\n", value.methodName, value.displayHint, value.value));
        } else {
            srcWriter.append(String.format("  return SnmpGet%sWith(params, values, idx, %s)\r\n", value.methodName, value.value));
        }
        srcWriter.append("}\r\n");
        if(!value.methodName.startsWith("Time")) {
            srcWriter.append("func SnmpRead").append(symbol.getName());
            srcWriter.append(String.format("FromOid(params sampling.MContext, oid []int) (%s, []int, error) {\r\n", value.name));
            if (null != value.size && !value.size.isEmpty()) {
                srcWriter.append(String.format("  return SnmpReadFixed%sFromOid(params, oid, %s, \"%s\")\r\n",
                        value.methodName, value.size, value.displayHint));
            } else if (null != value.displayHint && value.displayHint.isEmpty()) {
                srcWriter.append(String.format("  return SnmpRead%sWithDisplayHintAndDefaultValue(params, oid,  \"%s\")\r\n", value.methodName, value.displayHint));
            } else {
                srcWriter.append(String.format("  return SnmpRead%sFromOid(params, oid)\r\n", value.methodName));
            }
            srcWriter.append("}\r\n");
        }
        srcWriter.append("\r\n");
        srcWriter.append("\r\n");
    }

    @Override
    public void GenerateGoArray(MibValueSymbol valueSymbol, MibValueSymbol[] children) throws IOException {
        if(!is_only_types) {
            GenerateMetaTable(valueSymbol, children);
            GenerateGoTable(valueSymbol, children);
        }
    }

    @Override
    public void GenerateGoObject(MibValueSymbol valueSymbol, MibValueSymbol[] children) throws IOException {
        if(!is_only_types) {
            groups.put(valueSymbol.getParent().getName(), valueSymbol.getParent());
        }
    }

    private GoStringValue toGoType(SnmpTextualConvention type) {
        MibTypeSymbol symbol = type.getSyntax().getReferenceSymbol();
        if( type.getSyntax() instanceof IntegerType) {
            if(null != symbol) {
                if ("Counter".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Uint", "uint", "0");
                }
                if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Unsigned32", "uint32", "0");
                }
                if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Uint", "uint", "0");
                }
                if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Uint64", "uint64", "0");
                }
                if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Gauge32", "uint32", "0");
                }
                if ("GAUGE".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Gauge32", "uint32", "0");
                }
                if ("Integer32".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("Integer32", "int32", "0");
                }
                if ("TimeTicks".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("TimeTicks", "time.Duration", "0");
                }
                if ("TimeInterval".equalsIgnoreCase(symbol.getName())) {
                    return new GoStringValue("TimeInterval", "time.Duration", "0");
                }
                return new GoStringValue(symbol.getName(), "int", "0");
            }
            return new GoStringValue("Int", "int", "0");
        } else if( type.getSyntax() instanceof StringType) {
            if ("MacAddress".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("MacAddress", "string", "\"\"");
            }
            if ("PhysAddress".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("PhysAddress", "string", "\"\"");
            }
            if ("IpAddress".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("IpAddress", "string", "\"\"");
            }
            if ("SnmpAdminString".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("SnmpAdminString", "string", "\"\"");
            }
            if ("OwnerString".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("OwnerString", "string", "\"\"");
            }
            if ("DisplayString".equalsIgnoreCase(type.getSyntax().getName())) {
                return new GoStringValue("DisplayString", "string", "\"\"");
            }

            //noinspection Duplicates
            if("OCTET STRING".equalsIgnoreCase(type.getSyntax().getName())) {
                Constraint constraint = ((StringType) type.getSyntax()).getConstraint();
                if(constraint instanceof SizeConstraint) {
                    SizeConstraint size = (SizeConstraint) constraint;
                    if(null != size.getValues() && size.getValues().size() == 1 && !size.getValues().get(0).toString().contains("..")) {
                        return new GoStringValue("OctetString", "string", "\"\"", type.getDisplayHint(), size.getValues().get(0).toString());
                    }
                }
                return new GoStringValue("OctetString", "string", "\"\"", type.getDisplayHint(), null);
            }
            //noinspection Duplicates
            if("Opaque".equalsIgnoreCase(type.getSyntax().getName())) {
                Constraint constraint = ((StringType) type.getSyntax()).getConstraint();
                if(constraint instanceof SizeConstraint) {
                    SizeConstraint size = (SizeConstraint) constraint;
                    if(null != size.getValues() && size.getValues().size() == 1 && !size.getValues().get(0).toString().contains("..")) {
                        return new GoStringValue("OpaqueString", "string", "\"\"", type.getDisplayHint(), size.getValues().get(0).toString());
                    }
                }
                return new GoStringValue("OpaqueString", "string", "\"\"", type.getDisplayHint(), null);
            }
            return new GoStringValue("String", "string", "\"\"");
        } else if( type.getSyntax() instanceof BitSetType) {
            return new GoStringValue("Bits", "string", "\"\"");
        } else if( type.getSyntax() instanceof ObjectIdentifierType) {
            return new GoStringValue("Oid", "string", "\"\"");
        }
        throw new RuntimeException(type.toString() + "is unsupported.");
    }

    private String toGoMethod(MibValueSymbol el, MibValueSymbol prev_el) {
        return toGoMethod(el, prev_el, "old_row", "\"" + Integer.toString(((ObjectIdentifierValue) el.getValue()).getValue())+ "\"");
    }

    private TypeValue toMetaType(MibType type) {
        if(type instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) type;
            MibTypeSymbol symbol = objectType.getReferenceSymbol();
            String comment = null;
            if(null != symbol) {
                comment = symbol.getName();
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
                    if ("TimeTicks".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("duration", comment);
                    }
                    if ("TimeStamp".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("duration", comment);
                    }
                    if ("TimeInterval".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("duration", comment);
                    }
                    if ("MacAddress".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("ipAddress", comment);
                    }
                    if ("PhysAddress".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("physicalAddress", comment);
                    }
                    if ("DisplayString".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("string", comment);
                    }
                    if ("TimeInterval".equalsIgnoreCase(symbol.getName())) {
                        return new TypeValue("datetime", comment);
                    }
                    if(symbol.getType() instanceof SnmpTextualConvention) {
                        symbol = ((SnmpTextualConvention) symbol.getType()).getSyntax().getReferenceSymbol();
                    } else {
                        break;
                    }
                } while (null != symbol);
            }

            if( objectType.getSyntax() instanceof IntegerType) {
                return new TypeValue("integer", comment);
            } else if( objectType.getSyntax() instanceof StringType) {
                return new TypeValue("string", comment);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                return new TypeValue("string", comment);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                return new TypeValue("string", comment);
            } else if( objectType.getSyntax() instanceof RealType) {
                return new TypeValue("decimal", comment);
            } else if( objectType.getSyntax() instanceof BooleanType) {
                return new TypeValue("boolean", comment);
            }
        }
        throw new RuntimeException(type.toString() + "is unsupported.");
    }

    private boolean isPreRead(MibValueSymbol el) {
        if(el.getType() instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) el.getType();
            MibTypeSymbol symbol = objectType.getSyntax().getReferenceSymbol();
            if (null != symbol) {

                if ("InetAddressType".equalsIgnoreCase(symbol.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("Duplicates")
    private String toGoMethod(MibValueSymbol el, MibValueSymbol prev_el, String varName, String oid) {
        if(el.getType() instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) el.getType();

            MibTypeSymbol symbol = objectType.getSyntax().getReferenceSymbol();
            if(null != symbol) {
                if ("Counter".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetCounter32With(params, %s, %s, 0)", varName, oid);
                }
                if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetUnsigned32With(params, %s, %s, 0)", varName, oid);
                }
                if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetCounter32With(params, %s, %s, 0)", varName, oid);
                }
                if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetCounter64With(params, %s, %s, 0)", varName, oid);
                }
                if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetGauge32With(params, %s, %s, 0)", varName, oid);
                }
                if ("GAUGE".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetGauge32With(params, %s, %s, 0)",varName,  oid);
                }
                if ("Integer32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetInteger32With(params, %s, %s, 0)",varName,  oid);
                }
                if ("StorageType".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetStorageTypeWith(params, %s, %s, 0)", varName, oid);
                }
                if ("TimeStamp".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetTimeStampWith(params, %s, %s, 0)", varName, oid);
                }
                if ("TimeTicks".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetTimeTicksWith(params, %s, %s, 0)", varName, oid);
                }
                if ("TimeInterval".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetTimeIntervalWith(params, %s, %s, 0)", varName, oid);
                }
                if ("DateAndTime".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetDateAndTimeWith(params, %s, %s, 0)", varName, oid);
                }
                if ("TruthValue".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetTruthValueWith(params, %s, %s, 0)", varName, oid);
                }
                if ("TestAndIncr".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetTestAndIncrWith(params, %s, %s, 0)", varName, oid);
                }
                if ("MacAddress".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetMacAddressWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("PhysAddress".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetPhysAddressWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("SnmpAdminString".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetSnmpAdminStringWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("DisplayString".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetDisplayStringWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("OwnerString".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetOwnerStringWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("AutonomousType".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetAutonomousTypeWith(params, %s, %s)", varName, oid);
                }
                if ("InstancePointer".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetInstancePointerWith(params, %s, %s)", varName, oid);
                }
                if ("RowPointer".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetRowPointerWith(params, %s, %s)", varName, oid);
                }
                if ("IpAddress".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetIpAddressWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("InetAddress".equalsIgnoreCase(symbol.getName())) {
                    if(null == prev_el) {
                        return String.format("SnmpGetInetAddressWith(params, %s, %s, \"\")", varName, oid);
                    } else {
                        return String.format("SnmpGetInetAddressWithType(params, %s, %s, %s)", varName, oid, prev_el.getName());
                    }
                }
                if ("OCTET STRING".equalsIgnoreCase(symbol.getName())) {

//                    Constraint constraint = ((StringType) objectType.getSyntax()).getConstraint();
//                    if(constraint instanceof SizeConstraint) {
//                        SizeConstraint size = (SizeConstraint) constraint;
//                        if(null != size.getValues() && size.getValues().size() == 1) {
//                            return new GoStringValue("SnmpGetFixedOctetStringWith", "string", "\"\"", displayHint, size.getValues().get(0).toString());
//                        }
//                    }
//
//                    if(null != displayHint && !displayHint.isEmpty()) {
//                        return String.format("SnmpGetOctetStringWith(params, %s, %s, \"\")", varName, oid);
//                    }
                    return String.format("SnmpGetOctetStringWith(params, %s, %s, \"\")", varName, oid);
                }
                if ("Opaque".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpGetOpaqueStringWith(params, %s, %s, \"\")", varName, oid);
                }
                return String.format("SnmpGet%s(params, %s, %s)", symbol.getName(), varName, oid);
            }

            if( objectType.getSyntax() instanceof IntegerType) {
                return String.format("SnmpGetIntWith(params, %s, %s, 0)", varName, oid);
            } else if( objectType.getSyntax() instanceof StringType) {

                String displayHint= null;
                Constraint constraint = ((StringType) objectType.getSyntax()).getConstraint();
                if(constraint instanceof SizeConstraint) {
                    SizeConstraint size = (SizeConstraint) constraint;
                    if(null != size.getValues() && size.getValues().size() == 1) {
                        if( size.getValues().get(0) instanceof ValueConstraint) {
                            ValueConstraint valueConstraint = (ValueConstraint)size.getValues().get(0);
                            return String.format("SnmpGetFixedOctetStringWith(params, %s, %s, %s, \"%s\", \"\")", varName, oid, valueConstraint.getValue().toObject(), displayHint);
                        }
                    }
                }
                if(null != displayHint && !displayHint.isEmpty()) {
                    return String.format("SnmpGetOctetStringWithDisplayHintAndDefaultValue(params, %s, %s, %s, \"%s\", \"\")", varName, oid, displayHint);
                }
                return String.format("SnmpGetStringWith(params, %s, %s, \"\")", varName, oid);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                return String.format("SnmpGetStringWith(params, %s, %s, \"\")", varName, oid);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                return String.format("SnmpGetOidWith(params, %s, %s, \"\")", varName, oid);
            }
        }
        throw new RuntimeException(el.getType().toString() + "is unsupported.");
    }


    @SuppressWarnings("Duplicates")
    private String toGoReadMethod(MibValueSymbol el, MibValueSymbol prev_el, String varName) {
        if(el.getType() instanceof SnmpObjectType) {
            SnmpObjectType objectType = (SnmpObjectType) el.getType();
            MibTypeSymbol symbol = objectType.getSyntax().getReferenceSymbol();
            if(null != symbol) {
                if ("Counter".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadtCounter32FromOid(params, %s)", varName);
                }
                if ("Unsigned32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadUnsigned32FromOid(params, %s)", varName);
                }
                if ("Counter32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadCounter32FromOid(params, %s)", varName);
                }
                if ("Counter64".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadCounter64FromOid(params, %s)", varName);
                }
                if ("GAUGE32".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadGauge32FromOid(params, %s)", varName);
                }
                if ("GAUGE".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadGauge32FromOid(params, %s)", varName);
                }
                if ("InetAddress".equalsIgnoreCase(symbol.getName())) {
                    return String.format("SnmpReadInetAddressFromOid(params, %s, %s)", varName, prev_el.getName());
                }
                return String.format("SnmpRead%sFromOid(params, %s)", symbol.getName(), varName);
            }

            if( objectType.getSyntax() instanceof IntegerType) {
                return String.format("SnmpReadIntFromOid(params, %s)", varName);
            } else if( objectType.getSyntax() instanceof StringType) {
                return String.format("SnmpReadStringFromOid(params, %s)", varName);
            } else if( objectType.getSyntax() instanceof BitSetType) {
                return String.format("SnmpReadBitsFromOid(params, %s)", varName);
            } else if( objectType.getSyntax() instanceof ObjectIdentifierType) {
                return String.format("SnmpReadOidFromOid(params, %s)", varName);
            }
        }
        throw new RuntimeException(el.getType().toString() + "is unsupported.");
    }

    @Override
    public void Close() throws IOException {
        if(!groups.isEmpty()) {
            for(MibValueSymbol symbol : groups.values()) {
                GenerateMetaObject(symbol, symbol.getChildren());
                GenerateGoObjectCode(symbol, symbol.getChildren());
            }
        }
        if(!tables.isEmpty()) {
            String moduleName = toGoName(module);

            if(null != metaWriter) {
                srcWriter.append("func init() {\r\n");
            } else {
                srcWriter.append("func Register").append(moduleName).append("() {\r\n");
            }

            for (Map.Entry<String, MetricSpec> entry : tables.entrySet()) {
                srcWriter.append(" sampling.RegisterRouteSpec(\"").append(entry.getKey()).append("_default\", \"get\", \"")
                        .append(this.managedObject).append("\", \"").append(entry.getValue().metric).append("\", \"\", nil,\r\n")
                        .append("    func(rs *sampling.RouteSpec, params map[string]interface{}) (sampling.Method, error) {\r\n")
                        .append("      drv := &").append(entry.getValue().implName).append("{}\r\n")
                        .append("      return drv, drv.Init(rs, params)\r\n")
                        .append("    })\r\n");
            }
            srcWriter.append("}\r\n\r\n");


            srcWriter.append("var ").append(moduleName).append(" = []MibModule{\r\n");
            for(Map.Entry<String, MetricSpec> entry : tables.entrySet()) {
                srcWriter.append("  {Name: \"").append(entry.getKey()).append("\", IsArray: ")
                        .append(entry.getValue().isArray?"true":"false").append("},\r\n");
            }
            srcWriter.append("}\r\n");
        }

        if(null != metaWriter) {
            metaWriter.append("</metricDefinitions>");
            metaWriter.close();
        }
        srcWriter.close();
    }

    private void GenerateGoObjectCode(MibValueSymbol symbol, MibValueSymbol[] children) throws IOException {
        children = toLeafOnly(children);
        if(0 == children.length) {
            return;
        }

        srcWriter.append(String.format("type %s struct {\r\n  metrics.SnmpBase\r\n}\r\n\n", symbol.getName()));
        srcWriter.append(String.format("func (self *%s) Call(params sampling.MContext) sampling.Result {\r\n", symbol.getName()));

        srcWriter.append("  oids := []string{");
        for(MibValueSymbol el : children) {
            srcWriter.append("    \"");
            srcWriter.append(el.getValue().toString());
            srcWriter.append(".0\",\r\n");
        }
        srcWriter.append("  }\r\n");
        srcWriter.append("  values, e := self.Get(params, oids)\r\n");
        srcWriter.append("  if nil != e {\r\n");
        srcWriter.append("    return self.Return(nil, e)\r\n");
        srcWriter.append("  }\r\n");

        int i = 0;
        for (MibValueSymbol el : children) {
            if(isPreRead(el)) {
                srcWriter.append(String.format("  %s := %s\r\n",
                        el.getName(), toGoMethod(el, null, "values", String.format("oids[%d]", i))));
            }
            i ++;
        }

        srcWriter.append("  return self.OK(map[string]interface{}{\r\n");

        i = 0;
        MibValueSymbol prev_el = null;
        for (MibValueSymbol el : children) {
            if(isPreRead(el)) {
                srcWriter.append(String.format("    \"%s\":         %s,\r\n", el.getName(), el.getName()));
                prev_el = el;
            } else {
                srcWriter.append(String.format("    \"%s\":         %s,\r\n",
                        el.getName(), toGoMethod(el, prev_el, "values", String.format("oids[%d]", i))));
            }
            i ++;
        }
        srcWriter.append("  })\r\n");

        srcWriter.append("}\r\n\r\n");
        srcWriter.flush();

        tables.put(symbol.getName(), new MetricSpec(symbol.getName(), symbol.getName(), false));
    }



    private static MibValueSymbol[] toLeafOnly(MibValueSymbol[] children) {
        ArrayList<MibValueSymbol> results = new ArrayList<>();
        for(MibValueSymbol symbol : children) {
            if(!(symbol.getType() instanceof SnmpObjectType)) {
                continue;
            }
            if (((SnmpObjectType)symbol.getType()).getAccess().canRead()) {
                results.add(symbol);
            }
        }
        return results.toArray(new MibValueSymbol[results.size()]);
    }

    private static String toGoName(String module) {
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
        public String size;
        public String displayHint;

        public GoStringValue(String methodName, String name, String value) {
            this.methodName = methodName;
            this.name = name;
            this.value = value;
        }

        public GoStringValue(String methodName, String name, String value, String displayHint, String size) {
            this.methodName = methodName;
            this.name = name;
            this.value = value;
            this.displayHint = displayHint;
            this.size = size;
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
