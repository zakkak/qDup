package io.hyperfoil.tools.qdup;

import io.hyperfoil.tools.qdup.config.yaml.HostDefinition;
import io.hyperfoil.tools.qdup.config.yaml.HostDefinitionConstruct;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.qdup.shell.AbstractShell;
import io.hyperfoil.tools.qdup.shell.ContainerShell;
import io.hyperfoil.tools.yaup.json.Json;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class LocalTest extends SshTestBase{

    @Test
    public void pipelineSlit_nosplit(){
        List<List<String>> list = new ArrayList<List<String>>();
        Local.pipelineSplit("foobarbiz",list);
        assertEquals("list should have 1 entry",1,list.size());
        List<String> pipe = list.get(0);
        assertEquals("pipeline[0] should have 1 entry",1,pipe.size());
        String entry = pipe.get(0);
        assertEquals("pipeline[0][0] should be full input","foobarbiz",entry);
    }
    @Test
    public void pipelineSlit_space_split(){
        List<List<String>> list = new ArrayList<List<String>>();
        Local.pipelineSplit("foo bar",list);
        assertEquals("list should have 1 entry",1,list.size());
        List<String> pipe = list.get(0);
        assertEquals("pipeline[0] should have 2 entries: "+pipe,2,pipe.size());
        assertEquals("pipeline[0][0] should be full input","foo",pipe.get(0));
        assertEquals("pipeline[0][1] should be full input","bar",pipe.get(1));
    }
    @Test
    public void pipelineSlit_pipe_spaced_split(){
        List<List<String>> list = new ArrayList<List<String>>();
        Local.pipelineSplit("foo | bar",list);
        assertEquals("list should have 2 entries",2,list.size());
        List<String> pipe = list.get(0);
        assertEquals("pipeline[0]entries: "+pipe,1,pipe.size());
        assertEquals("pipeline[0][0] should be full input","foo",pipe.get(0));
        pipe = list.get(1);
        assertEquals("pipeline[1]entries: "+pipe,1,pipe.size());
        assertEquals("pipeline[1][0] should be full input","bar",pipe.get(0));
    }
    @Test
    public void pipelineSlit_pipe_no_space_split(){
        List<List<String>> list = new ArrayList<List<String>>();
        Local.pipelineSplit("foo|bar",list);
        assertEquals("list should have 1 entry",2,list.size());
        List<String> pipe = list.get(0);
        assertEquals("pipeline[0] entries: "+pipe,1,pipe.size());
        assertEquals("pipeline[0][0] should be full input","foo",pipe.get(0));
        pipe = list.get(1);
        assertEquals("pipeline[1] entries: "+pipe,1,pipe.size());
        assertEquals("pipeline[1][0] should be full input","bar",pipe.get(0));
    }
    @Test
    public void pipelineSlit_quoted_space(){
        List<List<String>> list = new ArrayList<List<String>>();
        Local.pipelineSplit("foo \"bar bar\"",list);
        assertEquals("list should have 1 entry",1,list.size());
        List<String> pipe = list.get(0);
        assertEquals("pipeline[0] should have 2 entries: "+pipe,2,pipe.size());
        assertEquals("pipeline[0][0] should be full input","foo",pipe.get(0));
        assertEquals("pipeline[0][1] should be full input","bar bar",pipe.get(1));
    }

    //TODO load from a url in the container to pass without internet connection
    @Test
    public void getRemote_url_https(){
        Local local = new Local(null);
        String content = local.getRemote("https://raw.githubusercontent.com/Hyperfoil/qDup/master/src/main/resources/sample.yaml");
        assertNotNull(content);
        assertTrue(content,content.length() > 0);
    }

    @Test
    public void getRemote_url(){
        Local local = new Local(null);
        String content = local.getRemote("raw.githubusercontent.com/Hyperfoil/qDup/master/src/main/resources/sample.yaml");
        assertNotNull(content);
        assertTrue(content,content.length() > 0);
    }

    @Test
    public void remote_ssh_filesize(){
        Host host = getHost();
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            Files.write(toSend.toPath(),"foobarbizbuz".getBytes());

            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));

            local.upload(toSend.getPath(),"/tmp/destination.txt",host);
            assertTrue("/tmp/destination.txt exists",exists("/tmp/destination.txt"));

            long response = local.remoteFileSize("/tmp/destination.txt",host);
            assertTrue("should ready back more than 0 bytes but was "+response,response > 0);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }
    @Test
    public void local_filesize(){
        Host host = new Host();
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();

            Files.write(toSend.toPath(),"foobarbizbuz".getBytes());

            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
            long response = local.remoteFileSize(toSend.getPath(),host);
            assertTrue("should read back more than 0 bytes but was "+response,response > 0);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }
    @Test
    public void remote_ssh_upload(){
        Host host = getHost();
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            Files.write(toSend.toPath(),"foo".getBytes());

            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));

            local.upload(toSend.getPath(),"/tmp/destination.txt",host);
            assertTrue("/tmp/destination.txt exists",exists("/tmp/destination.txt"));


            String read = readFile("/tmp/destination.txt");
            assertEquals("foo",read);

        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }
    @Test
    public void remote_container_upload_file(){
        Json hostJson = getHost().toJson();
        hostJson.set("container","quay.io/fedora/fedora");
        hostJson.set("platform","docker");
        HostDefinition hostDefinition = new HostDefinition(hostJson);
        Host host = hostDefinition.toHost(new State(""));        
        assertFalse(host.isLocal());
        assertTrue(host.isContainer());
        File toSend = null;
        //first we need to create the container
        AbstractShell shell = AbstractShell.getShell(
                host,
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
        );
        assertTrue("shell should be open",shell.isOpen());
        assertTrue("shell should be ready",shell.isReady());
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            Files.write(toSend.toPath(),"foo".getBytes());
            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
            local.upload(toSend.getPath(),"/tmp/foo.txt",host);
            String lsOutput = shell.shSync("ls -l /tmp");
            assertTrue("/tmp/foo.txt should exist in container",lsOutput.contains("foo.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            ContainerShell containerShell = new ContainerShell(
                host,
                "",
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
            );
            containerShell.stopContainerIfStarted();            
        }
    }    

    @Test
    public void local_upload_file(){
        Host host = new Host();//creates a local host
        assertTrue(host.isLocal());
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            toRead = File.createTempFile("tmp","local");
            toRead.deleteOnExit();

            Files.write(toSend.toPath(),"foo".getBytes());
            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));

            local.upload(toSend.getPath(),toRead.getPath(),host);
            assertTrue(toRead.getPath()+" should exist",toRead.exists());

            String read = readLocalFile(toRead.toPath());
            assertEquals("foo",read);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }
    @Test
    public void local_container_upload_file(){
        Host host = Host.parse(Host.LOCAL+Host.CONTAINER_SEPARATOR+"quay.io/fedora/fedora");
        assertTrue(host.isLocal());
        assertTrue(host.isContainer());
        File toSend = null;
        //first we need to create the container
        AbstractShell shell = AbstractShell.getShell(
                host,
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
        );
        assertTrue("shell should be open",shell.isOpen());
        assertTrue("shell should be ready",shell.isReady());
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            Files.write(toSend.toPath(),"foo".getBytes());
            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
            local.upload(toSend.getPath(),"/tmp/foo.txt",host);
            String lsOutput = shell.shSync("ls -l /tmp");
            assertTrue("/tmp/foo.txt should exist in container",lsOutput.contains("foo.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            ContainerShell containerShell = new ContainerShell(
                host,
                "",
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
            );
            containerShell.stopContainerIfStarted();            
        }
    }    
    @Test
    public void local_download(){
        Host host = new Host();//creates a local host
        assertTrue(host.isLocal());
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            toRead = File.createTempFile("tmp","local");
            toRead.deleteOnExit();

            Files.write(toSend.toPath(),"foo".getBytes());
            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));

            local.download(toSend.getPath(),toRead.getPath(),host);
            assertTrue(toRead.getPath()+" should exist",toRead.exists());

            String read = readLocalFile(toRead.toPath());
            assertEquals("foo",read);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }

    @Test
    public void local_container_download_file(){
        Host host = Host.parse("quay.io/fedora/fedora");
        AbstractShell shell = AbstractShell.getShell(
            host,
            "",
            new ScheduledThreadPoolExecutor(2),
            new SecretFilter(),
            false
        );
        String response = shell.shSync("echo 'foo' > /tmp/foo.txt");
        response = shell.shSync("ls -al /tmp/foo.txt");
        File toRead = null;
        Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
        try{
            toRead = File.createTempFile("tmp","local");
            //toRead.deleteOnExit();
            local.download("/tmp/foo.txt",toRead.getPath(),host);
            assertTrue("downloaded file should exist",toRead.exists());
            String read = readLocalFile(toRead.toPath());
            assertEquals("foo",read);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            ContainerShell containerShell = new ContainerShell(
                host,
                "",
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
            );
            containerShell.stopContainerIfStarted();
        }
    }
    //TODO how do we truly test remote when testcontainers run containers locally?
    @Test
    public void remote_container_download_file(){
        Json hostJson = getHost().toJson();
        hostJson.set("container","quay.io/fedora/fedora");
        hostJson.set("platform","docker");
        HostDefinition hostDefinition = new HostDefinition(hostJson);
        Host host = hostDefinition.toHost(new State(""));
        AbstractShell shell = AbstractShell.getShell(
                host,
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
        );
        assertTrue("shell should be open",shell.isOpen());
        assertTrue("shell should be ready",shell.isReady());
        String response = shell.shSync("echo 'foo' > /tmp/foo.txt");
        response = shell.shSync("ls -al /tmp/foo.txt");
        Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
        assertFalse("/tmp/foo.txt should not exist in testcontainer",exists("/tmp/foo.txt"));
        try{
            
            File toRead = File.createTempFile("tmp","local");
            toRead.deleteOnExit();
            local.download("/tmp/foo.txt",toRead.getPath(),host);
            assertTrue("downloaded file should exist",toRead.exists());
            String read = readLocalFile(toRead.toPath());
            assertEquals("foo",read);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {}
    }
    @Test
    public void remote_container_download_folder(){
        Json hostJson = getHost().toJson();
        hostJson.set("container","quay.io/fedora/fedora");
        hostJson.set("platform","docker");
        HostDefinition hostDefinition = new HostDefinition(hostJson);
        Host host = hostDefinition.toHost(new State(""));
        AbstractShell shell = AbstractShell.getShell(
                host,
                new ScheduledThreadPoolExecutor(2),
                new SecretFilter(),
                false
        );
        assertTrue("shell should be open",shell.isOpen());
        assertTrue("shell should be ready",shell.isReady());
        String containerDir = shell.shSync("mktemp -d");
        String response = "";
        response = shell.shSync("echo 'foo' > "+containerDir+"/foo.txt");
        response = shell.shSync("echo 'bar' > "+containerDir+"/bar.txt");
        response = shell.shSync("ls -al "+containerDir);
        Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));
        assertFalse("/tmp/foo.txt should not exist in testcontainer",exists("/tmp/foo.txt"));
        try{
            
            File dest = Files.createTempDirectory("tmp").toFile();
            dest.deleteOnExit();
            //add / to download content not folder
            local.download(containerDir+"/",dest.getPath(),host);
            assertTrue("downloaded file should exist",dest.exists());
            File foo = Path.of(dest.getPath(), "foo.txt").toFile();
            File bar = Path.of(dest.getPath(), "bar.txt").toFile();

            assertTrue("foo should exist",foo.exists());
            assertTrue("bar should exist",bar.exists());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {}
    }    
    @Test
    public void remote_ssh_download(){
        Host host = getHost();
        File toSend = null;
        File toRead = null;
        try {
            toSend = File.createTempFile("tmp","local");
            toSend.deleteOnExit();
            Files.write(toSend.toPath(),"foo".getBytes());

            Local local = new Local(getBuilder().buildConfig(Parser.getInstance()));

            local.upload(toSend.getPath(),"/tmp/destination.txt",host);
            assertTrue("/tmp/destination.txt exists",exists("/tmp/destination.txt"));


            String read = readFile("/tmp/destination.txt");
            assertEquals("foo",read);

            toRead = File.createTempFile("tmp","local");
            toRead.deleteOnExit();

            local.download("/tmp/destination.txt",toRead.getPath(),host);

            read = readLocalFile(toRead.toPath());
            assertEquals("foo",read);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(toSend !=null && toSend.exists()){
                toSend.delete();
            }
            if(toRead !=null && toRead.exists()){
                toRead.delete();
            }
        }
    }
}
