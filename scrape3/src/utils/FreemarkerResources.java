package utils;

import java.util.Locale;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class FreemarkerResources
{
	public static final String THREAD_TEMPLATE_FILENAME = "post.ftl";
	private static FreemarkerResources instance;
	private Configuration cfg;
	
	private FreemarkerResources()
	{
		cfg = new Configuration(Configuration.VERSION_2_3_28);
		cfg.setClassForTemplateLoading(FreemarkerResources.class, "/templates/");
		cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_20);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}
	
	public synchronized static FreemarkerResources getInstance()
	{
		if(instance == null)
		{
			instance = new FreemarkerResources();
		}
		return instance;
	}
	
	public synchronized Configuration getConfiguration()
	{
		return cfg;
	}
}