package org.molgenis.ui.menu;

import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.ui.MolgenisUi;
import org.molgenis.ui.MolgenisUiMenu;
import org.springframework.beans.factory.annotation.Autowired;

public class MenuMolgenisUi implements MolgenisUi
{
	private static final String KEY_HREF_LOGO = "app.href.logo";
	private static final String KEY_HREF_CSS = "app.href.css";
	private static final String KEY_TITLE = "app.href.logo";

	private final MolgenisSettings molgenisSettings;
	private final MenuReaderService menuReaderService;

	@Autowired
	public MenuMolgenisUi(MolgenisSettings molgenisSettings, MenuReaderService menuReaderService)
	{
		if (molgenisSettings == null) throw new IllegalArgumentException("molgenisSettings is null");
		this.molgenisSettings = molgenisSettings;
		this.menuReaderService = menuReaderService;
	}

	@Override
	public String getTitle()
	{
		return molgenisSettings.getProperty(KEY_TITLE);
	}

	@Override
	public String getHrefLogo()
	{
		return molgenisSettings.getProperty(KEY_HREF_LOGO);
	}

	@Override
	public String getHrefCss()
	{
		return molgenisSettings.getProperty(KEY_HREF_CSS);
	}

	@Override
	public MolgenisUiMenu getMenu()
	{
		Menu menu = menuReaderService.getMenu();
		return new MenuItemToMolgenisUiMenuAdapter(menu);
	}

	@Override
	public MolgenisUiMenu getMenu(String menuId)
	{
		Menu menu = findMenu(menuReaderService.getMenu(), menuId);
		return menu != null ? new MenuItemToMolgenisUiMenuAdapter(menu) : null;
	}

	private Menu findMenu(Menu menu, String menuId)
	{
		if (menuId.equals(menu.getId())) return menu;
		for (MenuItem menuItem : menu.getItems())
		{
			if (menuItem.getType() == MenuItemType.MENU)
			{
				if (!(menuItem instanceof Menu))
				{
					throw new RuntimeException("Invalid type " + menuItem.getClass().getSimpleName() + ", expected "
							+ Menu.class.getSimpleName());
				}
				Menu submenu = findMenu((Menu) menuItem, menuId);
				if (submenu != null)
				{
					return submenu;
				}
			}
		}
		return null;
	}
}
