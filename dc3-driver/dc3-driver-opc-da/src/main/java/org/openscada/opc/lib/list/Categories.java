/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
 */

package org.openscada.opc.lib.list;

public interface Categories {
    /**
     * Category of the OPC DA 1.0 Servers
     */
    public final static Category OPCDAServer10 = new Category(org.openscada.opc.dcom.common.Categories.OPCDAServer10);

    /**
     * Category of the OPC DA 2.0 Servers
     */
    public final static Category OPCDAServer20 = new Category(org.openscada.opc.dcom.common.Categories.OPCDAServer20);

    /**
     * Category of the OPC DA 3.0 Servers
     */
    public final static Category OPCDAServer30 = new Category(org.openscada.opc.dcom.common.Categories.OPCDAServer30);

    /**
     * Category of the XML DA 1.0 Servers
     */
    public final static Category XMLDAServer10 = new Category(org.openscada.opc.dcom.common.Categories.XMLDAServer10);
}
