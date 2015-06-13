/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.neutron.northbound.api;

import java.net.HttpURLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronCRUDInterfaces;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;

/**
 * Neutron Northbound REST APIs.<br>
 * This class provides REST APIs for managing neutron port objects
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in
 * tomcat-server.xml after adding a proper keystore / SSL certificate from a
 * trusted authority.<br>
 * More info :
 * http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */

@Path("/ports")
public class NeutronPortsNorthbound {

    final String mac_regex="^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    static private final int HTTP_OK_BOTTOM = 200;
    static private final int HTTP_OK_TOP = 299;

    private NeutronPort extractFields(NeutronPort o, List<String> fields) {
        return o.extractFields(fields);
    }

    @Context
    UriInfo uriInfo;

    /**
     * Returns a list of all Ports */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response listPorts(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("network_id") String queryNetworkID,
            @QueryParam("name") String queryName,
            @QueryParam("admin_state_up") String queryAdminStateUp,
            @QueryParam("status") String queryStatus,
            @QueryParam("mac_address") String queryMACAddress,
            @QueryParam("device_id") String queryDeviceID,
            @QueryParam("device_owner") String queryDeviceOwner,
            @QueryParam("tenant_id") String queryTenantID,
            // linkTitle
            @QueryParam("limit") Integer limit,
            @QueryParam("marker") String marker,
            @DefaultValue("false") @QueryParam("page_reverse") Boolean pageReverse
            // sorting not supported
            ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronPort> allPorts = portInterface.getAllPorts();
        List<NeutronPort> ans = new ArrayList<NeutronPort>();
        Iterator<NeutronPort> i = allPorts.iterator();
        while (i.hasNext()) {
            NeutronPort oSS = i.next();
            if ((queryID == null || queryID.equals(oSS.getID())) &&
                    (queryNetworkID == null || queryNetworkID.equals(oSS.getNetworkUUID())) &&
                    (queryName == null || queryName.equals(oSS.getName())) &&
                    (queryAdminStateUp == null || queryAdminStateUp.equals(oSS.getAdminStateUp())) &&
                    (queryStatus == null || queryStatus.equals(oSS.getStatus())) &&
                    (queryMACAddress == null || queryMACAddress.equals(oSS.getMacAddress())) &&
                    (queryDeviceID == null || queryDeviceID.equals(oSS.getDeviceID())) &&
                    (queryDeviceOwner == null || queryDeviceOwner.equals(oSS.getDeviceOwner())) &&
                    (queryTenantID == null || queryTenantID.equals(oSS.getTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(oSS,fields));
                } else {
                    ans.add(oSS);
                }
            }
        }

        if (limit != null && ans.size() > 1) {
            // Return a paginated request
            NeutronPortRequest request = (NeutronPortRequest) PaginatedRequestFactory.createRequest(limit,
                    marker, pageReverse, uriInfo, ans, NeutronPort.class);
            return Response.status(HttpURLConnection.HTTP_OK).entity(request).build();
        }

        return Response.status(HttpURLConnection.HTTP_OK).entity(
                new NeutronPortRequest(ans)).build();
    }

    /**
     * Returns a specific Port */

    @Path("{portUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response showPort(
            @PathParam("portUUID") String portUUID,
            // return fields
            @QueryParam("fields") List<String> fields ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        if (fields.size() > 0) {
            NeutronPort ans = portInterface.getPort(portUUID);
            return Response.status(HttpURLConnection.HTTP_OK).entity(
                    new NeutronPortRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(HttpURLConnection.HTTP_OK).entity(
                    new NeutronPortRequest(portInterface.getPort(portUUID))).build();
        }
    }

    /**
     * Creates new Ports */

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_CREATED, condition = "Created"),
        @ResponseCode(code = HttpURLConnection.HTTP_BAD_REQUEST, condition = "Bad Request"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_FORBIDDEN, condition = "Forbidden"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_CONFLICT, condition = "Conflict"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "MAC generation failure"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response createPorts(final NeutronPortRequest input) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException("Network CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronPort singleton = input.getSingleton();

            /*
             * the port must be part of an existing network, must not already exist,
             * have a valid MAC and the MAC not be in use
             */
            if (singleton.getNetworkUUID() == null) {
                throw new BadRequestException("network UUID musy be specified");
            }
            if (portInterface.portExists(singleton.getID())) {
                throw new BadRequestException("port UUID already exists");
            }
            if (!networkInterface.networkExists(singleton.getNetworkUUID())) {
                throw new ResourceNotFoundException("network UUID does not exist.");
            }
            if (singleton.getMacAddress() == null ||
                    !singleton.getMacAddress().matches(mac_regex)) {
                throw new BadRequestException("MAC address not properly formatted");
            }
            if (portInterface.macInUse(singleton.getMacAddress())) {
                throw new ResourceConflictException("MAC Address is in use.");
            }
            /*
             * if fixed IPs are specified, each one has to have an existing subnet ID
             * that is in the same scoping network as the port.  In addition, if an IP
             * address is specified it has to be a valid address for the subnet and not
             * already in use
             */
            List<Neutron_IPs> fixedIPs = singleton.getFixedIPs();
            if (fixedIPs != null && fixedIPs.size() > 0) {
                Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
                while (fixedIPIterator.hasNext()) {
                    Neutron_IPs ip = fixedIPIterator.next();
                    if (ip.getSubnetUUID() == null) {
                        throw new BadRequestException("subnet UUID not specified");
                    }
                    NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                    if (subnet == null) {
                        throw new BadRequestException("subnet UUID must exist");
                    }
                    if (!singleton.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                        throw new BadRequestException("network UUID must match that of subnet");
                    }
                    if (ip.getIpAddress() != null) {
                        if (!subnet.isValidIP(ip.getIpAddress())) {
                            throw new BadRequestException("IP address is not valid");
                        }
                        if (subnet.isIPInUse(ip.getIpAddress())) {
                            throw new ResourceConflictException("IP address is in use.");
                        }
                    }
                }
            }

            Object[] instances = NeutronUtil.getInstances(INeutronPortAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronPortAware service = (INeutronPortAware) instance;
                        int status = service.canCreatePort(singleton);
                        if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                            return Response.status(status).build();
                        }
                    }
                } else {
                    throw new ServiceUnavailableException("No providers registered.  Please try again later");
                }
            } else {
                throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
            }

            // add the port to the cache
            portInterface.addPort(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    service.neutronPortCreated(singleton);
                }
            }
        } else {
            List<NeutronPort> bulk = input.getBulk();
            Iterator<NeutronPort> i = bulk.iterator();
            HashMap<String, NeutronPort> testMap = new HashMap<String, NeutronPort>();
            Object[] instances = NeutronUtil.getInstances(INeutronPortAware.class, this);
            while (i.hasNext()) {
                NeutronPort test = i.next();

                /*
                 * the port must be part of an existing network, must not already exist,
                 * have a valid MAC and the MAC not be in use.  Further the bulk request
                 * can't already contain a new port with the same UUID
                 */
                if (portInterface.portExists(test.getID())) {
                    throw new BadRequestException("port UUID already exists");
                }
                if (testMap.containsKey(test.getID())) {
                    throw new BadRequestException("port UUID already exists");
                }
                for (NeutronPort check : testMap.values()) {
                    if (test.getMacAddress().equalsIgnoreCase(check.getMacAddress())) {
                        throw new ResourceConflictException("MAC address already allocated");
                    }
                    for (Neutron_IPs test_fixedIP : test.getFixedIPs()) {
                        for (Neutron_IPs check_fixedIP : check.getFixedIPs()) {
                            if (test_fixedIP.getSubnetUUID().equals(check_fixedIP.getSubnetUUID())) {
                                if (test_fixedIP.getIpAddress().equals(check_fixedIP.getIpAddress())) {
                                    throw new ResourceConflictException("IP address already allocated");
                                }
                            }
                        }
                    }
                }
                testMap.put(test.getID(), test);
                if (!networkInterface.networkExists(test.getNetworkUUID())) {
                    throw new ResourceNotFoundException("network UUID does not exist.");
                }
                if (!test.getMacAddress().matches(mac_regex)) {
                    throw new BadRequestException("MAC address not properly formatted");
                }
                if (portInterface.macInUse(test.getMacAddress())) {
                    throw new ResourceConflictException("MAC address in use");
                }

                /*
                 * if fixed IPs are specified, each one has to have an existing subnet ID
                 * that is in the same scoping network as the port.  In addition, if an IP
                 * address is specified it has to be a valid address for the subnet and not
                 * already in use (or be the gateway IP address of the subnet)
                 */
                List<Neutron_IPs> fixedIPs = test.getFixedIPs();
                if (fixedIPs != null && fixedIPs.size() > 0) {
                    Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
                    while (fixedIPIterator.hasNext()) {
                        Neutron_IPs ip = fixedIPIterator.next();
                        if (ip.getSubnetUUID() == null) {
                            throw new BadRequestException("subnet UUID must be specified");
                        }
                        if (!subnetInterface.subnetExists(ip.getSubnetUUID())) {
                            throw new BadRequestException("subnet UUID doesn't exists");
                        }
                        NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                        if (!test.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                            throw new BadRequestException("network UUID must match that of subnet");
                        }
                        if (ip.getIpAddress() != null) {
                            if (!subnet.isValidIP(ip.getIpAddress())) {
                                throw new BadRequestException("ip address not valid");
                            }
                            //TODO: need to add consideration for a fixed IP being assigned the same address as a allocated IP in the
                            //same bulk create
                            if (subnet.isIPInUse(ip.getIpAddress())) {
                                throw new ResourceConflictException("IP address in use");
                            }
                        }
                    }
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance : instances) {
                            INeutronPortAware service = (INeutronPortAware) instance;
                            int status = service.canCreatePort(test);
                            if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                                return Response.status(status).build();
                            }
                        }
                    } else {
                        throw new ServiceUnavailableException("No providers registered.  Please try again later");
                    }
                } else {
                    throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
                }
            }

            //once everything has passed, then we can add to the cache
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronPort test = i.next();
                portInterface.addPort(test);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronPortAware service = (INeutronPortAware) instance;
                        service.neutronPortCreated(test);
                    }
                }
            }
        }
        return Response.status(HttpURLConnection.HTTP_CREATED).entity(input).build();
    }

    /**
     * Updates a Port */

    @Path("{portUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackPorts.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_BAD_REQUEST, condition = "Bad Request"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_FORBIDDEN, condition = "Forbidden"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_CONFLICT, condition = "Conflict"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response updatePort(
            @PathParam("portUUID") String portUUID,
            NeutronPortRequest input
            ) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        INeutronSubnetCRUD subnetInterface = NeutronCRUDInterfaces.getINeutronSubnetCRUD( this);
        if (subnetInterface == null) {
            throw new ServiceUnavailableException("Subnet CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // port has to exist and only a single delta is supported
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        NeutronPort target = portInterface.getPort(portUUID);
        if (!input.isSingleton()) {
            throw new BadRequestException("only singleton edit suported");
        }
        NeutronPort singleton = input.getSingleton();
        NeutronPort original = portInterface.getPort(portUUID);

        // deltas restricted by Neutron
        if (singleton.getID() != null || singleton.getTenantID() != null ||
                singleton.getStatus() != null) {
            throw new BadRequestException("attribute change blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronPortAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    int status = service.canUpdatePort(singleton, original);
                    if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
            }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }

        // Verify the new fixed ips are valid
        List<Neutron_IPs> fixedIPs = singleton.getFixedIPs();
        if (fixedIPs != null && fixedIPs.size() > 0) {
            Iterator<Neutron_IPs> fixedIPIterator = fixedIPs.iterator();
            while (fixedIPIterator.hasNext()) {
                Neutron_IPs ip = fixedIPIterator.next();
                if (ip.getSubnetUUID() == null) {
                    throw new BadRequestException("subnet UUID must be specified");
                }
                if (!subnetInterface.subnetExists(ip.getSubnetUUID())) {
                    throw new BadRequestException("subnet UUID doesn't exist.");
                }
                NeutronSubnet subnet = subnetInterface.getSubnet(ip.getSubnetUUID());
                if (!target.getNetworkUUID().equalsIgnoreCase(subnet.getNetworkUUID())) {
                    throw new BadRequestException("network UUID must match that of subnet");
                }
                if (ip.getIpAddress() != null) {
                    if (!subnet.isValidIP(ip.getIpAddress())) {
                        throw new BadRequestException("invalid IP address");
                    }
                    if (subnet.isIPInUse(ip.getIpAddress())) {
                        throw new ResourceConflictException("IP address in use");
                    }
                }
            }
        }

        //        TODO: Support change of security groups
        // update the port and return the modified object
        portInterface.updatePort(portUUID, singleton);
        NeutronPort updatedPort = portInterface.getPort(portUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronPortAware service = (INeutronPortAware) instance;
                service.neutronPortUpdated(updatedPort);
            }
        }
        return Response.status(HttpURLConnection.HTTP_OK).entity(
                new NeutronPortRequest(updatedPort)).build();

    }

    /**
     * Deletes a Port */

    @Path("{portUUID}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_NO_CONTENT, condition = "No Content"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_FORBIDDEN, condition = "Forbidden"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response deletePort(
            @PathParam("portUUID") String portUUID) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            throw new ServiceUnavailableException("Port CRUD Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        // port has to exist and not be owned by anyone.  then it can be removed from the cache
        if (!portInterface.portExists(portUUID)) {
            throw new ResourceNotFoundException("port UUID does not exist.");
        }
        NeutronPort port = portInterface.getPort(portUUID);
        if (port.getDeviceID() != null ||
                port.getDeviceOwner() != null) {
            Response.status(HttpURLConnection.HTTP_FORBIDDEN).build();
        }
        NeutronPort singleton = portInterface.getPort(portUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronPortAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronPortAware service = (INeutronPortAware) instance;
                    int status = service.canDeletePort(singleton);
                    if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException("No providers registered.  Please try again later");
            }
        } else {
            throw new ServiceUnavailableException("Couldn't get providers list.  Please try again later");
        }
        portInterface.removePort(portUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronPortAware service = (INeutronPortAware) instance;
                service.neutronPortDeleted(singleton);
            }
        }
        return Response.status(HttpURLConnection.HTTP_NO_CONTENT).build();
    }
}
