/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.neutron.northbound.api;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.neutron.spi.NeutronNetwork;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public final class NeutronNetworkRequest implements INeutronRequest<NeutronNetwork> {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name = "network")
    NeutronNetwork singleton;

    @XmlElement(name = "networks")
    List<NeutronNetwork> bulkRequest;

    @XmlElement(name = "networks_links")
    List<NeutronPageLink> links;

    NeutronNetworkRequest() {
    }

    NeutronNetworkRequest(List<NeutronNetwork> bulkRequest, List<NeutronPageLink> links) {
        this.bulkRequest = bulkRequest;
        this.links = links;
    }

    NeutronNetworkRequest(List<NeutronNetwork> bulk) {
        bulkRequest = bulk;
    }

    NeutronNetworkRequest(NeutronNetwork net) {
        singleton = net;
    }
}
