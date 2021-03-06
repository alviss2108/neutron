/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.neutron.spi.NeutronSFCFlowClassifier;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public final class NeutronSFCFlowClassifierRequest implements INeutronRequest<NeutronSFCFlowClassifier> {
    // See OpenStack Networking SFC (networking-sfc) API v1.0 Reference for description of
    // annotated attributes

    @XmlElement(name = "flowclassifier")
    NeutronSFCFlowClassifier singleton;

    @XmlElement(name = "flowclassifiers")
    List<NeutronSFCFlowClassifier> bulkRequest;

    NeutronSFCFlowClassifierRequest() {
    }

    NeutronSFCFlowClassifierRequest(List<NeutronSFCFlowClassifier> bulkRequest) {
        this.bulkRequest = bulkRequest;
    }

    NeutronSFCFlowClassifierRequest(NeutronSFCFlowClassifier sfcFlowClassifier) {
        this.singleton = sfcFlowClassifier;
    }
}
