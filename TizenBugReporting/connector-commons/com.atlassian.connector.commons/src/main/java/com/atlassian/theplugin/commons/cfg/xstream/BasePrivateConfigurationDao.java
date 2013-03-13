package com.atlassian.theplugin.commons.cfg.xstream;

import com.atlassian.theplugin.commons.cfg.ServerCfgFactoryException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.JDomReader;
import com.thoughtworks.xstream.io.xml.JDomWriter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public abstract class BasePrivateConfigurationDao<T> {
    private static final String ATLASSIAN_DIR_NAME = ".atlassian";
	private static final String ATLASSIAN_IDE_CONNECTOR_DIR_NAME = "ide-connector";
    
    public BasePrivateConfigurationDao() {
    }

    void writeXmlFile(final Element element, @NotNull final File outputFile) throws IOException {
        StringWriter sw = new StringWriter();
        new XMLOutputter(Format.getPrettyFormat()).output(element, sw);
        sw.flush();
        sw.close();
        String str = sw.toString();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile.getAbsolutePath())));
        out.write(str);
        out.flush();
        out.close();
    }

    static void saveJDom(final Object object, final Element rootElement) {
        if (object == null) {
            throw new NullPointerException("Serialized object cannot be null");
        }
        final JDomWriter writer = new JDomWriter(rootElement);
        final XStream xStream = JDomXStreamUtil.getProjectJDomXStream(true);
        xStream.marshal(object, writer);


    }

    public static String getPrivateCfgDirectoryPath() {
        return System.getProperty("user.home") + File.separator + ATLASSIAN_DIR_NAME
                + File.separator + ATLASSIAN_IDE_CONNECTOR_DIR_NAME;
    }

    abstract String getRootElementName();

    public Document createJDom(final T t) {
        Document document = new Document(new Element(getRootElementName()));
        saveJDom(t, document.getRootElement());
        return document;
    }

    protected static File getPrivateCfgDirectorySavePath() throws ServerCfgFactoryException {

        final File ideConnectorHomeDir = new File(getPrivateCfgDirectoryPath());
        if (ideConnectorHomeDir.exists() == false) {
            if (ideConnectorHomeDir.mkdirs() == false) {
                throw new ServerCfgFactoryException("Cannot create directory [" + ideConnectorHomeDir.getAbsolutePath() + "]");
            }
        }


        if (ideConnectorHomeDir.isDirectory() && ideConnectorHomeDir.canWrite()) {
            return ideConnectorHomeDir;
        }
        throw new ServerCfgFactoryException("[" + ideConnectorHomeDir.getAbsolutePath() + "] is not writable"
                + " or is not a directory");
    }

    protected static <T1> T1 loadJDom(final Element rootElement, Class<T1> clazz, Boolean saveAll)
            throws ServerCfgFactoryException {
		final int childCount = rootElement.getChildren().size();
		if (childCount != 1) {
			throw new ServerCfgFactoryException("Cannot travers JDom tree. Exactly one child node expected, but found ["
					+ childCount + "]");
		}
		final JDomReader reader = new JDomReader((Element) rootElement.getChildren().get(0));
		final XStream xStream = JDomXStreamUtil.getProjectJDomXStream(saveAll);
		try {
			return clazz.cast(xStream.unmarshal(reader));
		} catch (ClassCastException e) {
			throw new ServerCfgFactoryException("Cannot load " + clazz.getSimpleName() + " due to ClassCastException: "
					+ e.getMessage(), e);
		} catch (Exception e) {
			throw new ServerCfgFactoryException("Cannot load " + clazz.getSimpleName() + ": "
					+ e.getMessage(), e);
		}
	}

    public boolean isDirReady() throws ServerCfgFactoryException {
        		final File atlassianDir = getPrivateCfgDirectorySavePath();

		return (atlassianDir.isDirectory() && atlassianDir.canRead());
    }
  
}