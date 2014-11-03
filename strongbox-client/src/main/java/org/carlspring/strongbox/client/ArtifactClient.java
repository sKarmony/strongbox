package org.carlspring.strongbox.client;

import org.carlspring.maven.commons.util.ArtifactUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.artifact.Artifact;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mtodorov
 */
public class ArtifactClient
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactClient.class);

    public static final String MANAGEMENT_URL = "manage/artifact";

    private String host = "http://localhost";

    private int port = System.getProperty("port.jetty.listen") != null ?
                       Integer.parseInt(System.getProperty("port.jetty.listen")) :
                       48080;

    private String contextBaseUrl = "http://localhost:" + port;

    private String username = "maven";

    private String password = "password";


    public ArtifactClient()
    {
    }

    public void addArtifact(Artifact artifact,
                            String storage,
                            String repository,
                            InputStream is)
            throws ArtifactOperationException
    {
        String url = (contextBaseUrl != null ? contextBaseUrl : host + ":" + port ) +
                     "/storages/" + storage + "/" + repository + "/" +
                     ArtifactUtils.convertArtifactToPath(artifact);

        logger.debug("Deploying " + url + "...");

        String fileName = ArtifactUtils.getArtifactFileName(artifact);

        deployFile(is, url, fileName);
    }

    public void deployFile(InputStream is,
                           String url,
                           String fileName)
            throws ArtifactOperationException
    {
        put(is, url, fileName, MediaType.APPLICATION_OCTET_STREAM);
    }

    public void put(InputStream is,
                    String url,
                    String fileName,
                    String mediaType)
            throws ArtifactOperationException
    {
        String contentDisposition = "attachment; filename=\"" + fileName +"\"";

        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(url);
        setupAuthentication(resource);

        Response response = resource.request(mediaType)
                                    .header("Content-Disposition", contentDisposition)
                                    .put(Entity.entity(is, mediaType));

        int status = response.getStatus();

        if (status != 200)
        {
            throw new ArtifactOperationException("Failed to upload file!");
        }
    }

    /**
     * This method will deploy an artifact with a random length to the remote host.
     * NOTE: This artifacts file will not be a valid Maven one, but will exist for the sake of testing.
     *
     * @param artifact
     * @param repository
     * @param length
     * @throws ArtifactOperationException
     */
    public void addArtifact(Artifact artifact,
                            String repository,
                            long length)
            throws ArtifactOperationException
    {
        Client client = ClientBuilder.newClient();

        String url = host + ":" + port + "/" + MANAGEMENT_URL + "/" +
                     repository + "/state/EXISTS/length/" + length + "/" +
                     ArtifactUtils.convertArtifactToPath(artifact);

        logger.debug("Using " + url);

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        Response response = webResource.request(MediaType.TEXT_PLAIN).get();

        int status = response.getStatus();

        if (status != 200)
        {
            throw new ArtifactOperationException("Failed to create artifact!");
        }
    }

    public void getArtifact(Artifact artifact,
                            String repository)
            throws ArtifactTransportException,
                   IOException
    {
        Client client = ClientBuilder.newClient();

        String url = host + ":" + port + "/" + contextBaseUrl + "/" +
                     repository + "/" +
                     ArtifactUtils.convertArtifactToPath(artifact);

        logger.debug("Getting " + url + "...");

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        Response response = webResource.request(MediaType.TEXT_PLAIN).get();

        final InputStream is = response.readEntity(InputStream.class);

        int total = 0;
        int len;
        final int size = 4096;
        byte[] bytes = new byte[size];

        while ((len = is.read(bytes, 0, size)) != -1)
        {
            total += len;
        }

        logger.debug("Response code: " + response.getStatus() + ". Read: " + total + " bytes.");

        int status = response.getStatus();

        if (status != 200)
        {
            throw new ArtifactTransportException("Failed to resolve artifact!");
        }

        if (total == 0)
        {
            throw new ArtifactTransportException("Artifact size was zero!");
        }
    }

    public void deleteArtifact(Artifact artifact,
                               String storage,
                               String repository)
    {
        Client client = ClientBuilder.newClient();

        String url = getUrlForArtifact(artifact, storage, repository);

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        webResource.request().delete();
    }

    public void delete(String storage,
                       String repository,
                       String path)
    {
        delete(storage, repository, path, false);
    }

    public void delete(String storage,
                       String repository,
                       String path,
                       boolean force)
    {
        Client client = ClientBuilder.newClient();

        @SuppressWarnings("ConstantConditions")
        String url = host + ":" + port + "/storages/" + storage + "/" + repository + "/" + path +
                     (force ? "?force=" + force : "");

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        webResource.request().delete();
    }

    public void deleteTrash(String storage, String repository)
    {
        Client client = ClientBuilder.newClient();

        String url = getUrlForTrash(storage, repository);

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        webResource.request().delete();
    }

    public void deleteTrash()
    {
        Client client = ClientBuilder.newClient();

        String url = host + ":" + port + "/trash";

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        webResource.request().delete();
    }

    public boolean artifactExists(Artifact artifact,
                                  String storage,
                                  String repository)
            throws ResponseException
    {
        Response response = artifactExistsStatusCode(artifact, storage, repository);

        if (response.getStatus() == 200)
        {
            return true;
        }
        else if (response.getStatus() == 404)
        {
            return false;
        }
        else
        {
            throw new ResponseException(response.getStatusInfo().getReasonPhrase(), response.getStatus());
        }
    }

    public Response artifactExistsStatusCode(Artifact artifact,
                                                   String storage,
                                                   String repository)
            throws ResponseException
    {
        Client client = ClientBuilder.newClient();

        String url = getUrlForArtifact(artifact, storage, repository);

        logger.debug("Path to artifact: " + url);

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        return webResource.request(MediaType.APPLICATION_XML).get();
    }

    public boolean pathExists(String path)
    {
        Client client = ClientBuilder.newClient();

        String url = host + ":" + port + (path.startsWith("/") ? path : '/' + path);

        logger.debug("Path to artifact: " + url);

        WebTarget webResource = client.target(url);
        setupAuthentication(webResource);
        Response response = webResource.request(MediaType.APPLICATION_XML).get();

        return response.getStatus() == 200;
    }

    public String getUrlForArtifact(Artifact artifact,
                                    String storage,
                                    String repository)
    {
        return host + ":" + port + "/storages/" + storage + "/" +
               repository + "/" +
               ArtifactUtils.convertArtifactToPath(artifact);
    }

    public String getUrlForTrash(String storage, String repository)
    {
        return host + ":" + port + "/trash/" + storage + "/" + repository;
    }

    public void setupAuthentication(WebTarget target)
    {
        if (username != null && password != null)
        {
            target.register(HttpAuthenticationFeature.basic(username, password));
        }
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getContextBaseUrl()
    {
        return contextBaseUrl;
    }

    public void setContextBaseUrl(String contextBaseUrl)
    {
        this.contextBaseUrl = contextBaseUrl;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

}
