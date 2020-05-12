package net.percederberg.mibble.compiler;

import net.percederberg.mibble.*;
import net.percederberg.mibble.snmp.*;
import net.percederberg.mibble.type.NullType;
import net.percederberg.mibble.type.ObjectIdentifierType;
import net.percederberg.mibble.type.SequenceOfType;
import net.percederberg.mibble.type.SequenceType;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2015/11/23.
 */
public class Mib2Go {

    private final static String[] maybe = new String[]{"StandardMibs",
            "commonmibs",
            "standard-mibs",
            "common-mibs",
            "standard_mibs",
            "common_mibs",
            "Standard",
            "commons",
            "common",
            ".."+ File.separator +"StandardMibs",
            ".."+ File.separator +"commonmibs",
            ".."+ File.separator +"standard-mibs",
            ".."+ File.separator +"common-mibs",
            ".."+ File.separator +"standard_mibs",
            ".."+ File.separator +"common_mibs",
            ".."+ File.separator +"Standard",
            ".."+ File.separator +"commons",
            ".."+ File.separator +"common"};

    public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        ArrayList<String> dirs = new ArrayList<String>();
        ArrayList<String> tables  = new ArrayList<String>();
        File standardmibs = new File("D:\\mibs\\StandardMibs");
        if(standardmibs.exists()) {
            if(standardmibs.isDirectory()) {
                dirs.add(standardmibs.getAbsolutePath());
            }
        }

        String managedObject = "";
        String meta_out = null;
        String src_out = null;
        String module = null;
        String namespace = null;
        String manufacturer = null;
        String prefix = null;
        //String metric_prefix = null;
        boolean is_managedObject = false;
        boolean is_module = false;
        boolean is_dir = false;
        boolean is_tables = false;
        boolean is_meta_out = false;
        boolean is_src_out = false;
        boolean is_only_types = false;
        boolean is_namespace = false;
        boolean is_manufacturer = false;
        boolean not_registrer_metrics = false;
        boolean is_prefix = false;
        //boolean is_metric_prefix = false;

        if(null == args || 0 == args.length) {
            usage();
            return;
        }
        for(String s : args) {
            if (is_tables) {
                if(s.startsWith("-")) {
                    usage();
                    return;
                }
                tables.add(s);
            } else if("-not_registrer_metrics".equalsIgnoreCase(s)){
                not_registrer_metrics = true;
            } else if("-only-types".equalsIgnoreCase(s)){
                is_only_types = true;
            } else if("-dir".equalsIgnoreCase(s)) {
                is_dir = true;
            } else if(is_dir){
                dirs.add(s);
                is_dir = false;
            } else if("-module".equalsIgnoreCase(s)) {
                is_module = true;
            } else if(is_module){
                module = s;
                is_module = false;
            } else if("-mo".equalsIgnoreCase(s)) {
                is_managedObject = true;
            } else if(is_managedObject){
                managedObject = s;
                is_managedObject = false;
            } else if("-meta-out".equalsIgnoreCase(s)) {
                is_meta_out = true;
            } else if(is_meta_out){
                meta_out = s;
                is_meta_out = false;
            } else if("-src-out".equalsIgnoreCase(s)) {
                is_src_out = true;
            } else if(is_src_out){
                src_out = s;
                is_src_out = false;
            } else if("-manufacturer".equalsIgnoreCase(s)) {
                is_manufacturer = true;
            } else if(is_manufacturer){
                manufacturer = s;
                is_manufacturer = false;
            } else if("-prefix".equalsIgnoreCase(s)) {
                is_prefix = true;
            } else if(is_prefix){
                prefix = s;
                is_prefix = false;
//            } else if("-metric_prefix".equalsIgnoreCase(s)) {
//                is_metric_prefix = true;
//            } else if(is_metric_prefix){
//                metric_prefix = s;
//                is_metric_prefix = false;
            } else if("-ns".equalsIgnoreCase(s)) {
                is_namespace = true;
            } else if(is_namespace){
                namespace = s;
                is_namespace = false;
            } else {
                is_tables = true;
                tables.add(s);
            }
        }

        if(null == module || module.isEmpty()) {
            System.out.println("模块名是必须的!");
            usage();
            return;
        }

        MibLoader loader = new MibLoader();
        MibLoaderLog log = new MibLoaderLog();

        File lib_mibs = new File("lib/mibs");
        if(lib_mibs.exists() && lib_mibs.isDirectory()) {
            loader.addAllDirs(lib_mibs);
            loadMaybe(loader, log, lib_mibs);
        } else {
            logPrint("mibs dir '%s' is not exists.", lib_mibs.getAbsolutePath());
        }

        for(String mibs_dir : dirs) {
            //loadDir(loader, new File(s), log);

            File mibs_dir_file = new File(mibs_dir);
            if(!mibs_dir_file.exists()) {
                logPrint("load all mibs failed, '%s' is not exists.", mibs_dir_file.getAbsolutePath());
                return;
            }
            if(!mibs_dir_file.isDirectory()) {
                logPrint("load all mibs failed, '%s' is a directory.", mibs_dir_file.getAbsolutePath());
                return;
            }

            loadMaybe(loader, log, mibs_dir_file);

            loader.addAllDirs(mibs_dir_file);
            loadDir(loader, mibs_dir_file, log);
        }


        //for(MibLoaderLog.LogEntry entry : log.entries())
//          Mib mib = loader.getMib("IF-MIB");
//          if(null != mib ) {
//            System.out.println(mib.getFile());
//          }
//          System.out.println("load "+file.toString()+" failed:");
//          e.getlogPrint().printTo(System.out);

//        if(log.errorCount() > 0) {
//            StringWriter sw = new StringWriter();
//            log.printTo(new PrintWriter(sw));
//            logPrint("load mib failed:\r\n%s", sw);
//        }
        //}

        Mib[] mibs = loader.getAllMibs();
        if(null == mibs|| 0 == mibs.length) {
            logPrint("load all mibs failed, result of load is empty.");
            return;
        }
        {
            Mib mib = loader.getMib(module);
            if (null != mib) {

                String filename = getFileName(prefix, mib);
                File  metaFile = null;
                if (!not_registrer_metrics) {
                    metaFile =new File(meta_out, "mib_" +  filename + "-gen.xml");
                    // metaWriter = new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
                }

                Generator generator = new GeneratorImpl(prefix, namespace, managedObject, mib.getName(),
                        metaFile,
                        new File(src_out, "metric_mib_" +  filename + "-gen-mib.go"),
                        is_only_types);
                generator.setManufacturer(manufacturer);
                generateMib(mib, tables, generator);
                generator.Close();
                return;
            }
        }
        mibs = loader.getMib(new File(module));
        if(null != mibs) {
            for (Mib mib1 : mibs) {
                String filename = getFileName(prefix, mib1);

                File  metaFile = null;
                if (!not_registrer_metrics) {
                    metaFile =new File(meta_out, "mib_" + filename + "-gen.xml");
                }
                Generator generator = new GeneratorImpl(prefix, namespace, managedObject, mib1.getName(),
                        metaFile,
                        new File(src_out, "metric_mib_" +  filename + "-gen-mib.go"),
                        is_only_types);
                generator.setManufacturer(manufacturer);
                generateMib(mib1, tables, generator);
                generator.Close();
            }
            return;
        }

//        for(MibLoaderLog.LogEntry entry : log.entriesByFile(new File("D:\\mibs\\StandardMibs\\rfc4382 MPLS__BGP Layer 3 Virtual Private Network (VPN).mib"))) {
//            logPrint(entry.toString());
//        }

        for(MibLoaderLog.LogEntry entry : log.errorEntries()) {
           entry.toString(1, System.err);
        }

        System.out.println("‘"+module+"’ is not found.");
    }

    private static String getFileName(String prefix, Mib mib1) {
        String filename = mib1.getName();
        if(null != prefix) {
            String prefix_name = prefix;
            if(prefix_name.endsWith("-")) {
                prefix_name = prefix_name.substring(0, prefix_name.length() -1);
            }
            if(prefix_name.endsWith("_")) {
                prefix_name = prefix_name.substring(0, prefix_name.length() -1);
            }
            if(!filename.toLowerCase().startsWith(prefix_name.toLowerCase())) {
                filename = prefix + filename;
            }
        }
        return filename;
    }

    private static void generateMib(Mib mib, ArrayList<String> tables, Generator generator) throws IOException {
        List<MibSymbol> symbols = mib.getAllSymbols();
        for(MibSymbol symbol : symbols) {
            if(symbol instanceof MibTypeSymbol) {
                if((((MibTypeSymbol) symbol).getType() instanceof SequenceOfType) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SequenceType) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SnmpNotificationType) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SnmpNotificationGroup) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SnmpAgentCapabilities) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SnmpModuleCompliance) ||
                        (((MibTypeSymbol) symbol).getType() instanceof SnmpTrapType) ||
                        (((MibTypeSymbol) symbol).getType() instanceof NullType)) {
                    continue;
                }
                //if(((MibTypeSymbol) symbol).getType() instanceof SnmpTextualConvention) {
                    generator.GenerateGoType((MibTypeSymbol)symbol, ((MibTypeSymbol) symbol).getType());
                    continue;
                //}
            }
            if(hasSymbol(tables, symbol)) {
                generateTable(symbol, generator);
                generateGroup(symbol, generator);
            }
        }
    }

    private static void generateGroup(MibSymbol symbol, Generator generator) throws IOException {
        if(!(symbol instanceof MibValueSymbol)) {
            return;
        }
        MibValueSymbol valueSymbol = (MibValueSymbol) symbol;
        if(!(valueSymbol.getType() instanceof SnmpObjectType)) {
            return;
        }
        SnmpObjectType objectType = (SnmpObjectType)valueSymbol.getType();
        if((objectType.getSyntax() instanceof  SequenceType)) {
            return;
        }
        if((objectType.getSyntax() instanceof SequenceOfType)) {
            return;
        }

        MibValueSymbol parent = valueSymbol.getParent();
        if(!(parent.getType() instanceof ObjectIdentifierType) &&
                !(parent.getType() instanceof SnmpModuleIdentity)&&
                !(parent.getType() instanceof SnmpObjectIdentity)){
            return;
        }
        generator.GenerateGoObject(valueSymbol, valueSymbol.getChildren());
    }

    private static void generateTable(MibSymbol symbol, Generator generator) throws IOException {
        if(!(symbol instanceof MibValueSymbol)) {
            return;
        }
        MibValueSymbol valueSymbol = (MibValueSymbol) symbol;
        if(!(valueSymbol.getType() instanceof SnmpObjectType)) {
            return;
        }
        SnmpObjectType objectType = (SnmpObjectType)valueSymbol.getType();

        if(!(objectType.getSyntax() instanceof  SequenceType)) {
            return;
        }
        //MibValueSymbol parent = valueSymbol.getParent();
        //SequenceType sequenceType = ((SequenceType)objectType.getSyntax());
        generator.GenerateGoArray(valueSymbol, valueSymbol.getChildren());
    }
    private static boolean hasSymbol(ArrayList<String> tables, MibSymbol symbol) {
        if(tables.isEmpty()) {
            return true;
        }
        for(String table : tables) {
            if(table.equalsIgnoreCase(symbol.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void usage() {
        System.out.println("使用方法： java -jar xx.jar  [-dir 目录] -module 模块名 表名");
    }

    private static void loadDir(MibLoader loader, File mibs_dir_file, MibLoaderLog log) {
        File[] files = mibs_dir_file.listFiles();
        if(null == files || 0 == files.length) {
            return;
        }
        File excepted = new File("D:\\mibs\\StandardMibs\\rfc4382 MPLS__BGP Layer 3 Virtual Private Network (VPN).mib");
        for(File file : files) {
            if(file.isDirectory()) {
                loadDir(loader, file, log);
            } else {
                try {
                    String[] ss = file.getName().split("\\.");
                    String ext = "";
                    if (2 <= ss.length) {
                        ext = ss[ss.length-1];
                    }

                    if(null == ext ||
                            ext.isEmpty()||
                            "my".equalsIgnoreCase(ext)||
                            "txt".equalsIgnoreCase(ext)||
                            "mib".equalsIgnoreCase(ext)||
                            "trp".equalsIgnoreCase(ext)||
                            "smi".equalsIgnoreCase(ext)||
                            "smi".equalsIgnoreCase(ext)) {

                        MibLoaderLog tmpLog = new MibLoaderLog();
                        loader.load(file, tmpLog);
                        if(tmpLog.errorCount() > 0) {
                            for(MibLoaderLog.LogEntry logEntry : tmpLog.entries()) {
                                log.add(logEntry);
                            }
                        }
                    }
                } catch (IOException e){
                    logPrint(String.format("load '%s' failed, ", file), e);
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadMibs(String mibs_dir) {

//        for(Mib mib : mibs) {
//            Collection symbols = mib.getAllSymbols();
//            if(null == symbols || symbols.isEmpty()) {
//                continue;
//            }
//            for (Object obj : symbols) {
//                if (obj instanceof MibValueSymbol) {
//                    MibValueSymbol symbol = (MibValueSymbol)obj;
//                    if(symbol.getValue() instanceof ObjectIdentifierValue) {
//                        ObjectIdentifierValue oid = (ObjectIdentifierValue)symbol.getValue();
//                        oid2names.put(oid.toString(), toSymbol(symbol));
//                    }
//                }
//            }
//        }
    }

    private static void loadMaybe(MibLoader loader, MibLoaderLog log, File mibs_dir_file) {
        for(String s : maybe ) {
            File file = new File(mibs_dir_file, s);
            if (file.exists()) {
                loader.addAllDirs(file);
                loadDir(loader, file, log);
            }
        }
    }

    private static void logPrint(String fmt, Object... args) {
        System.out.println(String.format(fmt,args));
    }

    private static void logPrint(String fmt, Exception e) {
        System.err.println(fmt);
        e.printStackTrace(System.err);
    }

}


