package org.carlspring.strongbox.services;

import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * @author mtodorov
 */
public interface ConfigurationManagementService
{

    String getBaseUrl()
            throws IOException;

    void setBaseUrl(String baseUrl)
            throws IOException, JAXBException;

    int getPort()
            throws IOException;

    void setPort(int port)
            throws IOException, JAXBException;

    void addOrUpdateStorage(Storage storage)
            throws IOException, JAXBException;

    Storage getStorage(String storageId)
            throws IOException;

    void removeStorage(String storageId)
            throws IOException, JAXBException;

    void addOrUpdateRepository(String storageId, Repository repository)
            throws IOException, JAXBException;

    Repository getRepository(String storageId, String repositoryId)
            throws IOException;

    void removeRepository(String storageId, String repositoryId)
            throws IOException, JAXBException;

}
