// NAME
//      $RCSfile: Ipv6AddrTablePdu.java,v $
// DESCRIPTION
//      [given below in javadoc format]
// DELTA
//      $Revision: 1.3 $
// CREATED
//      $Date: 2006/11/29 16:12:48 $
// COPYRIGHT
//      Westhawk Ltd
// TO DO
//

package uk.co.westhawk.examplev1;

import uk.co.westhawk.snmp.stack.*;
import uk.co.westhawk.snmp.pdu.*;
import java.util.*;


/**
 * The class Ipv6AddrTablePdu.
 *
 * This file is auto generated by the StubBrowser utility, using Mibble.
 * See the uk/co/westhawk/stub/ directory.
 *
 * Make sure that you replace the package name and classname placeholders. 
 * Also, move this file to the correct package directory.
 * If these things are not done, this class will not compile correctly!!
 *
 * @version $Revision: 1.3 $ $Date: 2006/11/29 16:12:48 $
 */
public class Ipv6AddrTablePdu extends GetNextPdu
{
    private static final String version_id =
        "@(#)$Id: Ipv6AddrTablePdu.java,v 1.3 2006/11/29 16:12:48 birgit Exp $ Copyright Westhawk Ltd";

    public final static String ipv6AddrPfxLength_OID = "1.3.6.1.2.1.55.1.8.1.2";
    public final static String ipv6AddrType_OID = "1.3.6.1.2.1.55.1.8.1.3";
    public final static String ipv6AddrAnycastFlag_OID = "1.3.6.1.2.1.55.1.8.1.4";
    public final static String ipv6AddrStatus_OID = "1.3.6.1.2.1.55.1.8.1.5";

    public final static int NO_SCAL = 0;
    public final static int NO_COL = 4;
    public final static int NO_OID = NO_SCAL + NO_COL;


    public final static String scal_oids[] =
    {
    };

    public final static String col_oids[] =
    {
        ipv6AddrPfxLength_OID,
        ipv6AddrType_OID,
        ipv6AddrAnycastFlag_OID,
        ipv6AddrStatus_OID,
    };

    public final static String all_oids[] =
    {
        ipv6AddrPfxLength_OID,
        ipv6AddrType_OID,
        ipv6AddrAnycastFlag_OID,
        ipv6AddrStatus_OID,
    };


    protected Integer _ipv6AddrPfxLength;
    protected java.util.HashMap _ipv6AddrTypeMap = new java.util.HashMap(3);
    protected Integer _ipv6AddrType;
    protected java.util.HashMap _ipv6AddrAnycastFlagMap = new java.util.HashMap(2);
    protected Integer _ipv6AddrAnycastFlag;
    protected java.util.HashMap _ipv6AddrStatusMap = new java.util.HashMap(5);
    protected Integer _ipv6AddrStatus;

    protected boolean _invalid = false;
    protected int _tmpErrorInd = -1;
    protected int _tmpErrorStat = 0;

/**
 * Constructor.
 *
 * @param con The context of the request
 */
public Ipv6AddrTablePdu(SnmpContextBasisFace con)
{
    super(con);
    _ipv6AddrTypeMap.put(new Integer(3), "unknown");
    _ipv6AddrTypeMap.put(new Integer(2), "stateful");
    _ipv6AddrTypeMap.put(new Integer(1), "stateless");

    _ipv6AddrAnycastFlagMap.put(new Integer(1), "true");
    _ipv6AddrAnycastFlagMap.put(new Integer(2), "false");

    _ipv6AddrStatusMap.put(new Integer(4), "inaccessible");
    _ipv6AddrStatusMap.put(new Integer(5), "unknown");
    _ipv6AddrStatusMap.put(new Integer(2), "deprecated");
    _ipv6AddrStatusMap.put(new Integer(1), "preferred");
    _ipv6AddrStatusMap.put(new Integer(3), "invalid");

    _invalid = false;
    _tmpErrorInd = -1;
    _tmpErrorStat = 0;
}

/**
 * Constructor that will send the first request immediately.
 *
 * @param con The context of the request
 * @param o the Observer that will be notified when the answer is
 * received
 */
public Ipv6AddrTablePdu(SnmpContextBasisFace con, Observer o)
throws PduException, java.io.IOException
{
    this(con);
    addOids(null);
    if (o != null)
    {
        addObserver(o);
    }
    send();
}


/**
 * The method addOids is the basis for the GetNext functionality.
 *
 * If old is null, it initialises the varbinds from all_oids.
 * If old is not null, it copies the column OIDs from the
 * old Ipv6AddrTablePdu object.
 * so the request continues where the previous one left.
 *
 * Note, the scalars and the columns OIDs are handled differently. The
 * scalars are always copied from the original scal_oids, only the
 * column OIDs are copied from the old
 * Ipv6AddrTablePdu object.
 */
public void addOids(Ipv6AddrTablePdu old)
{
    if (old != null)
    {
        for (int i=0; i<NO_SCAL; i++)
        {
            addOid(scal_oids[i]);
        }
        for (int i=NO_SCAL; i<NO_OID; i++)
        {
            varbind var = (varbind) old.respVarbinds.elementAt(i);
            addOid(var.getOid());
        }
    }
    else
    {
        for (int i=0; i<NO_OID; i++)
        {
            addOid(all_oids[i]);
        }
    }
}


/**
 * This method sets the column index. By doing this, the request will
 * return (only) the row after row index.
 *
 * The index parameters only applies to the column OIDs.
 * The scalars are copied from the original scal_oids.
 */
public void addOids(int index)
{
    for (int i=0; i<NO_SCAL; i++)
    {
        addOid(scal_oids[i]);
    }
    for (int i=0; i<NO_COL; i++)
    {
        addOid(col_oids[i] + "." + index);
    }
}
/**
 * The value of the request is set. This will be called by
 * Pdu.fillin().
 *
 * I check if the variables are still in range.
 * I do this because I'm only interessed in a part of the MIB. If I
 * would not do this check, I'll get the whole MIB from the starting
 * point, instead of the variables in the table.
 *
 * @param n the index of the value
 * @param res the value
 * @see Pdu#new_value
 */
protected void new_value(int n, varbind res)
{
    if (getErrorStatus() == AsnObject.SNMP_ERR_NOERROR)
    {
        AsnObjectId oid = res.getOid();
        AsnObject value = res.getValue();

        if (oid.toString().startsWith(all_oids[n]))
        {
            try
            {
                switch (n)
                {
                    case 0:
                        setIpv6AddrPfxLength(value);
                        break;
                    case 1:
                        setIpv6AddrType(value);
                        break;
                    case 2:
                        setIpv6AddrAnycastFlag(value);
                        break;
                    case 3:
                        setIpv6AddrStatus(value);
                        break;
                    default:
                        _invalid = true;
                        setTmpErrorIndex(n);
                        _tmpErrorStat = SnmpConstants.SNMP_ERR_GENERR;
                }
            }
            catch (ClassCastException exc)
            {
                _invalid = true;
                setTmpErrorIndex(n);
                _tmpErrorStat = SnmpConstants.SNMP_ERR_GENERR;
            }
        }
        else
        {
            _invalid = true;
            setTmpErrorIndex(n);
            _tmpErrorStat = SnmpConstants.SNMP_ERR_NOSUCHNAME;
        }
    }

    if (n >= NO_OID-1)
    {
        if (_invalid == true)
        {
            setErrorStatus(_tmpErrorStat);
            setErrorIndex(_tmpErrorInd);
        }
    }
}


/**
ipv6AddrPfxLength<br/>
OBJECT-TYPE (
  Syntax: [UNIVERSAL 2] INTEGER (0..128)
  Units: bits
  Access: read-only
  Status: current
  Description: The length of the prefix (in bits) associated with
               the IPv6 address of this entry.
)<br/>
*/
public void setIpv6AddrPfxLength(AsnObject new_value)
{
    AsnInteger obj = (AsnInteger) new_value;
    _ipv6AddrPfxLength = new Integer(obj.getValue());
}
public Integer getIpv6AddrPfxLength()
{
    return _ipv6AddrPfxLength;
}


/**
ipv6AddrType<br/>
OBJECT-TYPE (
  Syntax: [UNIVERSAL 2] INTEGER (1 | 2 | 3)
  Access: read-only
  Status: current
  Description: The type of address. Note that 'stateless(1)'
               refers to an address that was statelessly
               autoconfigured; 'stateful(2)' refers to a address
               which was acquired by via a stateful protocol
               (e.g. DHCPv6, manual configuration).
)<br/>
*/
public void setIpv6AddrType(AsnObject new_value)
{
    AsnInteger obj = (AsnInteger) new_value;
    _ipv6AddrType = new Integer(obj.getValue());
}
public String getIpv6AddrTypeStr()
{
    String ret = null;
    if (_ipv6AddrType != null)
    {
        ret = (String) _ipv6AddrTypeMap.get(_ipv6AddrType);
    }
    return ret;
}
public Integer getIpv6AddrType()
{
    return _ipv6AddrType;
}


/**
ipv6AddrAnycastFlag<br/>
TYPE TruthValue ::= TEXTUAL-CONVENTION (
  Status: current
  Description: Represents a boolean value.
  Syntax: [UNIVERSAL 2] INTEGER (1 | 2)
)<br/>
OBJECT-TYPE (
  Syntax: [UNIVERSAL 2] INTEGER (1 | 2)
  Access: read-only
  Status: current
  Description: This object has the value 'true(1)', if this
               address is an anycast address and the value
               'false(2)' otherwise.
)<br/>
*/
public void setIpv6AddrAnycastFlag(AsnObject new_value)
{
    AsnInteger obj = (AsnInteger) new_value;
    _ipv6AddrAnycastFlag = new Integer(obj.getValue());
}
public String getIpv6AddrAnycastFlagStr()
{
    String ret = null;
    if (_ipv6AddrAnycastFlag != null)
    {
        ret = (String) _ipv6AddrAnycastFlagMap.get(_ipv6AddrAnycastFlag);
    }
    return ret;
}
public Integer getIpv6AddrAnycastFlag()
{
    return _ipv6AddrAnycastFlag;
}


/**
ipv6AddrStatus<br/>
OBJECT-TYPE (
  Syntax: [UNIVERSAL 2] INTEGER (1 | 2 | 3 | 4 | 5)
  Access: read-only
  Status: current
  Description: Address status.  The preferred(1) state indicates
               that this is a valid address that can appear as
               the destination or source address of a packet.
               The deprecated(2) state indicates that this is
               a valid but deprecated address that should no longer
               be used as a source address in new communications,
               but packets addressed to such an address are
               processed as expected. The invalid(3) state indicates
               that this is not valid address which should not
               
               appear as the destination or source address of
               a packet. The inaccessible(4) state indicates that
               the address is not accessible because the interface
               to which this address is assigned is not operational.
)<br/>
*/
public void setIpv6AddrStatus(AsnObject new_value)
{
    AsnInteger obj = (AsnInteger) new_value;
    _ipv6AddrStatus = new Integer(obj.getValue());
}
public String getIpv6AddrStatusStr()
{
    String ret = null;
    if (_ipv6AddrStatus != null)
    {
        ret = (String) _ipv6AddrStatusMap.get(_ipv6AddrStatus);
    }
    return ret;
}
public Integer getIpv6AddrStatus()
{
    return _ipv6AddrStatus;
}


private java.net.InetAddress getInetAddress(AsnOctets obj)
{
    java.net.InetAddress iad = null;
    try
    {
        iad = java.net.InetAddress.getByAddress(obj.getBytes());
    }
    catch(java.net.UnknownHostException exc) { }
    return iad;
}


/**
 * Returns if this set of values is invalid.
 */
public boolean isInvalid()
{
    return _invalid;
}


/**
 * Sets _tmpErrorInd, but only once.
 */
private void setTmpErrorIndex(int errind)
{
    if (_tmpErrorInd == -1)
    {
        _tmpErrorInd = errind;
    }
}


public String toString()
{
    StringBuffer buffer = new StringBuffer(getClass().getName());
    buffer.append("[");
    buffer.append("ipv6AddrPfxLength=").append(_ipv6AddrPfxLength);
    buffer.append(", ipv6AddrType=").append(getIpv6AddrTypeStr());
    buffer.append(", ipv6AddrAnycastFlag=").append(getIpv6AddrAnycastFlagStr());
    buffer.append(", ipv6AddrStatus=").append(getIpv6AddrStatusStr());
    buffer.append(", invalid=").append(_invalid);
    buffer.append("]");
    return buffer.toString();
}


}

