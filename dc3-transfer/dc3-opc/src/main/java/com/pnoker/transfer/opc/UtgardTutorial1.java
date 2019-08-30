package com.pnoker.transfer.opc;

import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.lib.list.Categories;
import org.openscada.opc.lib.list.Category;
import org.openscada.opc.lib.list.ServerList;

import java.net.UnknownHostException;
import java.util.Collection;

public class UtgardTutorial1 {

    public static void main(String[] args) throws JIException, UnknownHostException {
        ServerList serverList = new ServerList("localhost", "Administrator",
                "939510703", "");

        Collection<ClassDetails> classDetails = serverList
                .listServersWithDetails(new Category[]{
                        Categories.OPCDAServer10, Categories.OPCDAServer20,
                        Categories.OPCDAServer30}, new Category[]{});

        for (ClassDetails cds : classDetails) {
            System.out.println(cds.getProgId() + "=" + cds.getDescription());
        }
    }
}