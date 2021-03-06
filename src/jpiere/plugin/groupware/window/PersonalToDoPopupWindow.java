/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package jpiere.plugin.groupware.window;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WDatetimeEditor;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WYesNoEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.South;

import jpiere.plugin.groupware.model.MToDo;
import jpiere.plugin.groupware.model.MToDoTeam;
import jpiere.plugin.groupware.util.GroupwareToDoUtil;

/**
 * JPIERE-0473 Personal ToDo Popup Window
 *
 *
 * @author h.hagiwara
 *
 */
public class PersonalToDoPopupWindow extends Window implements EventListener<Event>,ValueChangeListener {

	private static final long serialVersionUID = 7757368164776005797L;

	private static final CLogger log = CLogger.getCLogger(PersonalToDoPopupWindow.class);

	private Properties ctx = null;

	/** Control Parameters	*/
	private boolean p_IsUpdatable = false;
	private boolean p_IsUpdatable_ToDoStatus = false;
	private boolean p_IsNewRecord = false;
	private boolean p_IsTeamToDo = false;
	private boolean p_RequeryOnCancel = false;

	private MToDo p_MToDo = null;
	private MToDoTeam p_TeamMToDo = null;
	private int p_JP_ToDo_ID = 0;

	private int p_AD_User_ID = 0;
	private int login_User_ID = 0;

	private String p_JP_ToDo_Type = null;
	private int p_JP_ToDo_Category_ID = 0;
	private Timestamp p_InitialScheduledStartTime = null;
	private Timestamp p_InitialScheduledEndTime = null;
	private boolean p_IsDirty = false;
	private boolean p_Debug = false;

	private I_CallerToDoPopupwindow i_CallerPersonalToDoPopupwindow;
	private  List<MToDo>  list_ToDoes = null;
	private int index = 0;


	/*** Web Components ***/
	// WEditors & Labels
	private Map<String, Label> map_Label = new HashMap<String, Label>();
	private Map<String, WEditor> map_Editor = new HashMap<String, WEditor>();

	//Layout
	private North north = null;
	private Center center = null;
	private ConfirmPanel confirmPanel;

	//Buttons
	private Button zoomBtn = null;
	private Button undoBtn = null;
	private Button saveBtn = null;
	private Button leftBtn = null;
	private Button rightBtn = null;
	private Button deleteBtn = null;

	//*** Constants ***//
	private final static String BUTTON_NAME_ZOOM = "ZOOM";
	private final static String BUTTON_NAME_UNDO = "REDO";
	private final static String BUTTON_NAME_SAVE = "SAVE";
	private final static String BUTTON_NAME_PREVIOUS_TODO = "PREVIOUS";
	private final static String BUTTON_NAME_NEXT_TODO = "NEXT";
	private final static String BUTTON_NAME_DELETE = "DELETE";

	public PersonalToDoPopupWindow(I_CallerToDoPopupwindow caller, int index)
	{
		super();
		this.i_CallerPersonalToDoPopupwindow = caller;
		this.list_ToDoes =caller.getPersonalToDoList();
		this.index = index;
		ctx = Env.getCtx();
		login_User_ID = Env.getAD_User_ID(ctx);

		setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		if (!ThemeManager.isUseCSSForWindowSize()) {
			ZKUpdateUtil.setWindowWidthX(this, 400);
			ZKUpdateUtil.setWindowHeightX(this, 600);
		} else {
			addCallback(AFTER_PAGE_ATTACHED, t -> {
				ZKUpdateUtil.setCSSHeight(this);
				ZKUpdateUtil.setCSSWidth(this);
			});
		}

		this.setSclass("popup-dialog request-dialog");
		this.setBorder("normal");
		this.setShadow(true);
		this.setClosable(true);

		if(index <= -1)
		{
			p_JP_ToDo_ID = 0;

		}else {

			p_JP_ToDo_ID = list_ToDoes.get(index).getJP_ToDo_ID();
		}

		updateControlParameter(p_JP_ToDo_ID);
		updateWindowTitle();

		createLabelMap();
		createEditorMap();
		updateEditorValue();

		Borderlayout borderlayout = new Borderlayout();
		this.appendChild(borderlayout);
		ZKUpdateUtil.setHflex(borderlayout, "1");
		ZKUpdateUtil.setVflex(borderlayout, "1");

		north = new North();
		north = updateNorth();
		borderlayout.appendChild(north);

		center = new Center();
		center.setSclass("dialog-content");
		center.setAutoscroll(true);
		center = updateCenter();
		borderlayout.appendChild(center);

		if(p_IsNewRecord)
		{
			South southPane = new South();
			southPane.setSclass("dialog-footer");
			borderlayout.appendChild(southPane);
			confirmPanel = new ConfirmPanel(true);
			confirmPanel.addActionListener(this);
			southPane.appendChild(confirmPanel);
		}
	}

	private void updateControlParameter(int JP_ToDo_ID)
	{
		p_JP_ToDo_ID = JP_ToDo_ID;

		if(p_JP_ToDo_ID == 0)
		{
			p_IsNewRecord = true;
			p_MToDo = null;
			p_AD_User_ID = i_CallerPersonalToDoPopupwindow.getDefault_AD__User_ID();
			p_JP_ToDo_Type = i_CallerPersonalToDoPopupwindow.getDefault_JP_ToDo_Type();
			p_JP_ToDo_Category_ID = i_CallerPersonalToDoPopupwindow.getDefault_JP_ToDo_Category_ID ();

		}else {

			p_IsNewRecord = false;
			p_MToDo = new MToDo(ctx, p_JP_ToDo_ID, null);
			p_AD_User_ID = p_MToDo.getAD_User_ID();
			p_JP_ToDo_Type = p_MToDo.getJP_ToDo_Type();
			p_JP_ToDo_Category_ID = p_MToDo.getJP_ToDo_Category_ID();
		}

		if(p_IsNewRecord)
		{
			p_IsUpdatable = true;

		}else {

			if(p_AD_User_ID == login_User_ID || p_MToDo.getCreatedBy() == login_User_ID)
			{
				if(p_MToDo.isProcessed())
					p_IsUpdatable = false;
				else
					p_IsUpdatable = true;

			}else {

				p_IsUpdatable = false;
			}
		}

		if(p_AD_User_ID == login_User_ID || p_MToDo.getCreatedBy() == login_User_ID)
		{
			p_IsUpdatable_ToDoStatus = true;
		}else {
			p_IsUpdatable_ToDoStatus = false;
		}

		if(p_IsNewRecord)
		{
			p_IsTeamToDo = false;
			p_TeamMToDo = null;
		}else if(p_MToDo.getJP_ToDo_Team_ID() == 0) {
			p_IsTeamToDo = false;
			p_TeamMToDo = null;
		}else {
			p_IsTeamToDo = true;
			p_TeamMToDo = new MToDoTeam(ctx, p_MToDo.getJP_ToDo_Team_ID(), null);
		}


		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
		{
			p_InitialScheduledStartTime = i_CallerPersonalToDoPopupwindow.getDefault_JP_ToDo_ScheduledStartTime();
			p_InitialScheduledEndTime = i_CallerPersonalToDoPopupwindow.getDefault_JP_ToDo_ScheduledEndTime();

		}else if(MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type)){

			p_InitialScheduledStartTime = null;
			p_InitialScheduledEndTime = i_CallerPersonalToDoPopupwindow.getDefault_JP_ToDo_ScheduledStartTime();

		}else {

			p_InitialScheduledStartTime = null;
			p_InitialScheduledEndTime = null;

		}
	}

	private void createLabelMap()
	{
		MTable m_table = MTable.get(ctx, MToDo.Table_Name);
		MColumn[] m_Columns = m_table.getColumns(false);
		String columnName = null;
		Label label = null;
		for(int i = 0; i < m_Columns.length; i++)
		{
			columnName = m_Columns[i].getColumnName();
			label = new Label(Msg.getElement(ctx, columnName));
			map_Label.put(columnName,label);
		}
	}

	private void createEditorMap()
	{
		//*** AD_User_ID ***//
		MLookup lookup_AD_User_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_AD_User_ID),  DisplayType.Search);
		WSearchEditor Editor_AD_User_ID = new WSearchEditor(lookup_AD_User_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_AD_User_ID), null, true, p_IsNewRecord? false : true, true);
		Editor_AD_User_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(Editor_AD_User_ID.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_AD_User_ID, Editor_AD_User_ID);


		//*** JP_ToDo_Type ***//
		MLookup lookup_JP_ToDo_Type = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name,  MToDo.COLUMNNAME_JP_ToDo_Type),  DisplayType.List);
		//WTableDirEditor editor_JP_ToDo_Type = new WTableDirEditor(lookup_JP_ToDo_Type, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Type), null, true, p_IsTeamToDo? true : !p_IsUpdatable, true);
		WTableDirEditor editor_JP_ToDo_Type = new WTableDirEditor(MToDo.COLUMNNAME_JP_ToDo_Type, true, p_IsTeamToDo? true : !p_IsUpdatable, true, lookup_JP_ToDo_Type);
		editor_JP_ToDo_Type.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Type.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Type, editor_JP_ToDo_Type);


		//*** JP_ToDo_Category_ID ***//
		String validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + p_AD_User_ID;
		MLookup lookup_JP_ToDo_Category_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Category_ID),  DisplayType.Search);
		lookup_JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
		WSearchEditor editor_JP_ToDo_Category_ID = new WSearchEditor(lookup_JP_ToDo_Category_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID), null, false, p_IsTeamToDo? true : !p_IsUpdatable, true);
		editor_JP_ToDo_Category_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Category_ID.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Category_ID, editor_JP_ToDo_Category_ID);


		//*** Name ***//
		WStringEditor editor_Name = new WStringEditor(MToDo.COLUMNNAME_Name, true, p_IsTeamToDo? true : !p_IsUpdatable, true, 30, 30, "", null);
		editor_Name.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_Name.getComponent(), "true");
		editor_Name.getComponent().setRows(2);
		map_Editor.put(MToDo.COLUMNNAME_Name, editor_Name);


		//*** Description ***//
		WStringEditor editor_Description = new WStringEditor(MToDo.COLUMNNAME_Description, true, p_IsTeamToDo? true : !p_IsUpdatable, true, 30, 30, "", null);
		editor_Description.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_Description.getComponent(), "true");
		editor_Description.getComponent().setRows(3);
		map_Editor.put(MToDo.COLUMNNAME_Description, editor_Description);


		//*** Comments ***//
		WStringEditor editor_Comments = new WStringEditor(MToDo.COLUMNNAME_Comments, true, !p_IsUpdatable, true, 30, 30, "", null);
		editor_Comments.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_Comments.getComponent(), "true");
		editor_Comments.getComponent().setRows(p_IsNewRecord? 2 : 3);
		map_Editor.put(MToDo.COLUMNNAME_Comments, editor_Comments);


		//*** JP_ToDo_ScheduledStartTime ***//
		WDatetimeEditor editor_JP_ToDo_ScheduledStartTime = new WDatetimeEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime, false, p_IsTeamToDo? true : !p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledStartTime.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledStartTime.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime, editor_JP_ToDo_ScheduledStartTime);


		//*** JP_ToDo_ScheduledEndTime ***//
		WDatetimeEditor editor_JP_ToDo_ScheduledEndTime = new WDatetimeEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime, false, p_IsTeamToDo? true :!p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledEndTime.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledEndTime.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime, editor_JP_ToDo_ScheduledEndTime);


		//*** JP_ToDo_Status ***//
		MLookup lookup_JP_ToDo_Status = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Status),  DisplayType.List);
		WTableDirEditor editor_JP_ToDo_Status = new WTableDirEditor(lookup_JP_ToDo_Status, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Status), null, true, p_IsUpdatable_ToDoStatus? false: true, true);//TODO
		editor_JP_ToDo_Status.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Status.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Status, editor_JP_ToDo_Status);

		//*** IsOpenToDoJP ***//
		WYesNoEditor editor_IsOpenToDoJP = new WYesNoEditor(MToDo.COLUMNNAME_IsOpenToDoJP, Msg.getElement(ctx, MToDo.COLUMNNAME_IsOpenToDoJP), null, true, !p_IsUpdatable, true);
		editor_IsOpenToDoJP.addValueChangeListener(this);
		map_Editor.put(MToDo.COLUMNNAME_IsOpenToDoJP, editor_IsOpenToDoJP);


		//*** JP_Statistics_YesNo  ***//
		MLookup lookup_JP_Statistics_YesNo = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_Statistics_YesNo),  DisplayType.List);
		WTableDirEditor editor_JP_Statistics_YesNo = new WTableDirEditor(lookup_JP_Statistics_YesNo, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_YesNo), null, false, !p_IsUpdatable, true);
		editor_JP_Statistics_YesNo.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_Statistics_YesNo.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_YesNo, editor_JP_Statistics_YesNo);


		//*** JP_Statistics_Choice ***//
		MLookup lookup_JP_Statistics_Choice = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_Statistics_Choice),  DisplayType.List);
		WTableDirEditor editor_JP_Statistics_Choice = new WTableDirEditor(lookup_JP_Statistics_Choice, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Choice), null, false, !p_IsUpdatable, true);
		editor_JP_Statistics_Choice.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_Statistics_Choice.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_Choice, editor_JP_Statistics_Choice);


		//*** JP_Statistics_DateAndTime ***//
		WDatetimeEditor editor_JP_Statistics_DateAndTime = new WDatetimeEditor(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_DateAndTime), null, false, !p_IsUpdatable, true);
		editor_JP_Statistics_DateAndTime.addValueChangeListener(this);
		ZKUpdateUtil.setHflex((HtmlBasedComponent)editor_JP_Statistics_DateAndTime.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_DateAndTime, editor_JP_Statistics_DateAndTime);


		//*** JP_Statistics_Number ***//
		WNumberEditor editor_JP_Statistics_Number = new WNumberEditor(MToDo.COLUMNNAME_JP_Statistics_Number, false, !p_IsUpdatable, true, DisplayType.Number, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Number));
		editor_JP_Statistics_Number.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_Statistics_Number.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_Number, editor_JP_Statistics_Number);
	}

	private void updateEditorStatus()
	{
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_Name).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_Description).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_Comments).setReadWrite(p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).setReadWrite(p_IsTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).setReadWrite(p_IsUpdatable_ToDoStatus? true: false);
		map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).setReadWrite(p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).setReadWrite(p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice).setReadWrite(p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).setReadWrite(p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number).setReadWrite(p_IsUpdatable);

	}

	private void updateEditorValue()
	{
		if(p_IsNewRecord)
		{

			map_Editor.get(MToDo.COLUMNNAME_AD_User_ID).setValue(p_AD_User_ID);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).setValue(p_JP_ToDo_Type);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).setValue(p_JP_ToDo_Category_ID);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setValue(p_InitialScheduledStartTime);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).setValue(p_InitialScheduledEndTime);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).setValue(MToDo.JP_TODO_STATUS_NotYetStarted);
			map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).setValue("Y");

		}else {

			MTable m_table = MTable.get(ctx, MToDo.Table_Name);
			MColumn[] m_Columns = m_table.getColumns(false);
			String columnName = null;
			WEditor editor = null;
			for(int i = 0; i < m_Columns.length; i++)
			{
				columnName = m_Columns[i].getColumnName();
				editor = map_Editor.get(columnName);
				if(editor != null)
				{
					if(m_Columns[i].getAD_Reference_ID()==DisplayType.YesNo)
					{
						editor.setValue(p_MToDo.isOpenToDoJP()==true ? "Y" : "N");
					}else{
						editor.setValue(p_MToDo.get_Value(columnName));
					}
				}
			}

		}
	}

	private Div createLabelDiv(Label label, boolean isMandatory )
	{
		label.setMandatory(isMandatory);
		Div div = new Div();
		div.setSclass("form-label");
		div.appendChild(label);
		if(isMandatory)
			div.appendChild(label.getDecorator());

		return div;
	}

	private void updateWindowTitle()
	{
		if(p_IsNewRecord)
		{
			setTitle("[" + Msg.getElement(ctx,MToDo.COLUMNNAME_JP_ToDo_ID) + "] " + Msg.getMsg(ctx, "NewRecord"));

		}else {
			if(p_MToDo.getJP_ToDo_Team_ID() == 0)
			{
				setTitle("[" + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ID) + "] "
						+ Msg.getElement(Env.getCtx(),MToDo.COLUMNNAME_CreatedBy)
						+ ":" + MUser.getNameOfUser(p_MToDo.getCreatedBy()));

			}else {

				setTitle("[" + Msg.getElement(ctx,MToDo.COLUMNNAME_JP_ToDo_Team_ID) + "] "
						+ Msg.getElement(Env.getCtx(),MToDo.COLUMNNAME_CreatedBy)
						+ ":" + MUser.getNameOfUser(p_MToDo.getCreatedBy()));
			}

		}

	}

	private North updateNorth()
	{
		if(north.getFirstChild() != null)
			north.getFirstChild().detach();

		if(p_IsNewRecord)
			return north;

		Hlayout hlyaout = new Hlayout();
		north.appendChild(hlyaout);

		Div div = new Div();
		hlyaout.appendChild(div);

		//Zoom Button
		if(zoomBtn == null)
		{
			zoomBtn = new Button();
			zoomBtn.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
			zoomBtn.setClass("btn-small");
			zoomBtn.setName(BUTTON_NAME_ZOOM);
			zoomBtn.addEventListener(Events.ON_CLICK, this);
		}
		hlyaout.appendChild(zoomBtn);

		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Undo Button
		if(undoBtn == null)
		{
			undoBtn = new Button();
			undoBtn.setImage(ThemeManager.getThemeResource("images/Undo16.png"));
			undoBtn.setClass("btn-small");
			undoBtn.setName(BUTTON_NAME_UNDO);
			undoBtn.addEventListener(Events.ON_CLICK, this);
		}
		if(p_IsDirty)
			undoBtn.setEnabled(true);
		else
			undoBtn.setEnabled(false);
		hlyaout.appendChild(undoBtn);


		//Save Button
		if(saveBtn == null)
		{
			saveBtn = new Button();
			saveBtn.setImage(ThemeManager.getThemeResource("images/Save16.png"));
			saveBtn.setClass("btn-small");
			saveBtn.setName(BUTTON_NAME_SAVE);
			saveBtn.addEventListener(Events.ON_CLICK, this);
		}
		if(p_IsDirty)
			saveBtn.setEnabled(true);
		else
			saveBtn.setEnabled(false);
		hlyaout.appendChild(saveBtn);

		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Left Button
		if(leftBtn  == null)
		{
			leftBtn = new Button();
			leftBtn.setImage(ThemeManager.getThemeResource("images/MoveLeft16.png"));
			leftBtn.setClass("btn-small");
			leftBtn.setName(BUTTON_NAME_PREVIOUS_TODO);
			leftBtn.addEventListener(Events.ON_CLICK, this);
		}
		hlyaout.appendChild(leftBtn);
		if(index == 0)
			leftBtn.setEnabled(false);
		else
			leftBtn.setEnabled(true);


		//Right Button
		if(rightBtn == null)
		{
			rightBtn = new Button();
			rightBtn.setImage(ThemeManager.getThemeResource("images/MoveRight16.png"));
			rightBtn.setClass("btn-small");
			rightBtn.addEventListener(Events.ON_CLICK, this);
			rightBtn.setName(BUTTON_NAME_NEXT_TODO);
		}
		hlyaout.appendChild(rightBtn);
		if(index == list_ToDoes.size()-1)
			rightBtn.setEnabled(false);
		else
			rightBtn.setEnabled(true);


		StringBuilder msg = new StringBuilder(p_IsDirty? "*" : "");
		msg = msg.append((index + 1) + " / " + list_ToDoes.size());

		if(p_Debug)
		{
			msg = msg.append(" | " + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ID) +  ":" + p_MToDo.getJP_ToDo_ID());

			if(p_IsTeamToDo)
			{
				msg = msg.append(" | " + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Team_ID) +  ":" + p_MToDo.getJP_ToDo_Team_ID());
			}
		}


		hlyaout.appendChild(GroupwareToDoUtil.createLabelDiv(null, msg.toString(),true));
		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Delete Button
		if(deleteBtn == null)
		{
			deleteBtn = new Button();
			deleteBtn.setImage(ThemeManager.getThemeResource("images/" + "Delete16.png"));
			deleteBtn.setClass("btn-small");
			deleteBtn.addEventListener(Events.ON_CLICK, this);
			deleteBtn.setName(BUTTON_NAME_DELETE);
		}
		deleteBtn.setEnabled(p_IsUpdatable);
		hlyaout.appendChild(deleteBtn);

		return north;

	}

	private Center updateCenter()
	{
		if(center.getFirstChild() != null)
			center.getFirstChild().detach();

		Div centerContent = new Div();
		center.appendChild(centerContent);
		ZKUpdateUtil.setVflex(center, "min");

		Grid grid = GridFactory.newGridLayout();
		ZKUpdateUtil.setVflex(grid, "min");
		ZKUpdateUtil.setHflex(grid, "1");
		centerContent.appendChild(grid);

		Rows rows = grid.newRows();

		//*** AD_User_ID ***//
		Row row = rows.newRow();
		rows.appendChild(row);
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_AD_User_ID), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_AD_User_ID).getComponent(),4);


		//*** JP_ToDo_Type ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Type), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).getComponent(),4);


		//*** JP_ToDo_Category_ID ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID), false),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).getComponent(),4);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).showMenu();


		//*** Name ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Name), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Name).getComponent(),4);


		//*** Description ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Description), false),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Description).getComponent(),4);


		//*** Comments ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Comments), false),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Comments).getComponent(),4);


		//*** JP_ToDo_ScheduledStartTime ***//
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
		{
			row = rows.newRow();
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime), true),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).getComponent(),4);
		}

		//*** JP_ToDo_ScheduledEndTime ***//
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type) || MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type) )
		{
			row = rows.newRow();
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime), true),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).getComponent(),4);
		}

		//*** JP_ToDo_Status ***//
		row = rows.newRow();
		row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Status), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).getComponent(),2);


		//*** IsOpenToDoJP ***//
		Div div_IsOpenToDoJP = new Div();
		div_IsOpenToDoJP.appendChild(map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).getComponent());
		row.appendCellChild(div_IsOpenToDoJP,2);


		/********************************************************************************************
		 * Statistics Info
		 ********************************************************************************************/

		row = rows.newRow();
		Groupbox statisticsInfo_GroupBox = new Groupbox();
		statisticsInfo_GroupBox.setOpen(true);
		row.appendCellChild(statisticsInfo_GroupBox,6);

		String caption = Msg.getMsg(Env.getCtx(),"JP_StatisticsInfo");
		if(p_TeamMToDo != null)
		{
			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_None.equals(p_TeamMToDo.getJP_Mandatory_Statistics_Info()))
			{

			}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_YesNo.equals(p_TeamMToDo.getJP_Mandatory_Statistics_Info())) {

				caption = caption + " [" + Msg.getElement(ctx, "IsMandatory") + ":" + map_Label.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).getValue() + "]";

			}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Choice.equals(p_TeamMToDo.getJP_Mandatory_Statistics_Info())) {

				caption = caption + " [" + Msg.getElement(ctx, "IsMandatory") + ":" + map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Choice).getValue() + "]";

			}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_DateAndTime.equals(p_TeamMToDo.getJP_Mandatory_Statistics_Info())) {

				caption = caption + " [" + Msg.getElement(ctx, "IsMandatory") + ":" + map_Label.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).getValue() + "]";

			}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Number.equals(p_TeamMToDo.getJP_Mandatory_Statistics_Info())) {

				caption = caption + " [" + Msg.getElement(ctx, "IsMandatory") + ":" + map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Number).getValue() + "]";

			}else {
				;
			}
		}

		statisticsInfo_GroupBox.appendChild(new Caption(caption));
		Grid statisticsInfo_Grid  = GridFactory.newGridLayout();
		statisticsInfo_Grid.setStyle("background-color: #E9F0FF");
		statisticsInfo_Grid.setStyle("border: none");
		statisticsInfo_GroupBox.appendChild(statisticsInfo_Grid);

		Rows statisticsInfo_rows = statisticsInfo_Grid.newRows();

		String JP_Mandatory_Statistics_Info = null;
		if(p_TeamMToDo!=null)
		{
			JP_Mandatory_Statistics_Info = p_TeamMToDo.getJP_Mandatory_Statistics_Info();
		}

		//*** JP_Statistics_YesNo  ***//
		row = statisticsInfo_rows.newRow();
		if(p_IsTeamToDo && MToDoTeam.JP_MANDATORY_STATISTICS_INFO_YesNo.equals(JP_Mandatory_Statistics_Info))
		{
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_YesNo), true),2);
		}else {
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_YesNo), false),2);
		}
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).getComponent(),4);


		//*** JP_Statistics_Choice ***//
		row = statisticsInfo_rows.newRow();
		if(p_IsTeamToDo && MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Choice.equals(JP_Mandatory_Statistics_Info))
		{
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Choice), true),2);
		}else{
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Choice), false),2);
		}
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice).getComponent(),4);


		//*** JP_Statistics_DateAndTime ***//
		row = statisticsInfo_rows.newRow();
		if(p_IsTeamToDo && MToDoTeam.JP_MANDATORY_STATISTICS_INFO_DateAndTime.equals(JP_Mandatory_Statistics_Info))
		{
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime), true),2);
		}else {
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime), false),2);
		}
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).getComponent(),4);


		//*** JP_Statistics_Number ***//
		row = statisticsInfo_rows.newRow();
		if(p_IsTeamToDo && MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Number.equals(JP_Mandatory_Statistics_Info))
		{
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Number), true),2);
		}else {
			row.appendCellChild(createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Number), false),2);
		}
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number).getComponent(),4);

		return center;
	}

	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();

		if (p_IsNewRecord && event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_OK))
		{

			if(saveToDo())
			{
				i_CallerPersonalToDoPopupwindow.refresh(p_JP_ToDo_Type);
				this.detach();
			}


		}
		else if (p_IsNewRecord && event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_CANCEL))
		{
			if(p_RequeryOnCancel)
			{
				i_CallerPersonalToDoPopupwindow.refresh(p_JP_ToDo_Type);
			}
			this.detach();

		}else{

			if(comp instanceof Button)
			{
				Button btn = (Button) comp;
				String btnName = btn.getName();
				if(BUTTON_NAME_PREVIOUS_TODO.equals(btnName))
				{
					if(p_IsDirty)
						saveToDo();

					index--;
					if(index >= 0 )
					{
						p_IsDirty = false;
						updateControlParameter(list_ToDoes.get(index).getJP_ToDo_ID());
						updateWindowTitle();
						updateEditorValue();
						updateEditorStatus();
						updateNorth();
						updateCenter();

					}else {
						index = 0;
						updateNorth();
					}

				}else if(BUTTON_NAME_NEXT_TODO.equals(btnName)){

					if(p_IsDirty)
						saveToDo();

					index++;
					if(index < list_ToDoes.size())
					{
						p_IsDirty = false;
						updateControlParameter(list_ToDoes.get(index).getJP_ToDo_ID());
						updateWindowTitle();
						updateEditorValue();
						updateEditorStatus();
						updateNorth();
						updateCenter();

					}else {
						index = list_ToDoes.size()-1;
						updateNorth();
					}

				}else if(BUTTON_NAME_ZOOM.equals(btnName)){

					AEnv.zoom(MTable.getTable_ID(MToDo.Table_Name), p_JP_ToDo_ID);
					this.detach();

				}else if(BUTTON_NAME_SAVE.equals(btnName)){

					saveToDo();

				}else if(BUTTON_NAME_DELETE.equals(btnName)){

					deleteToDo();

				}else if(BUTTON_NAME_UNDO.equals(btnName)) {

					p_IsDirty = false;
					//updateControlParameter(list_ToDoes.get(index).getJP_ToDo_ID());
					//updateWindowTitle();
					updateEditorValue();
					//updateEditorStatus();
					updateNorth();
					updateCenter();
				}

			}
		}
	}

	private boolean saveToDo()
	{
		if(!p_IsUpdatable && !p_IsUpdatable_ToDoStatus)
		{
			return true;
		}

		if(p_IsNewRecord)
			p_MToDo = new MToDo(Env.getCtx(), 0, null);

		p_MToDo.setAD_Org_ID(0);

		//Check AD_User_ID
		WEditor editor = map_Editor.get(MToDo.COLUMNNAME_AD_User_ID);
		if(editor.getValue() == null || ((Integer)editor.getValue()).intValue() == 0)
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_AD_User_ID);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_MToDo.setAD_User_ID((Integer)editor.getValue());
		}

		//Check JP_ToDo_Type
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_Type);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_MToDo.setJP_ToDo_Type((String)editor.getValue());
		}

		//Check JP_ToDo_Category_ID
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID);
		if(editor.getValue() == null || ((Integer)editor.getValue()).intValue() == 0)
		{
			;
		}else {
			p_MToDo.setJP_ToDo_Category_ID((Integer)editor.getValue());
		}

		//Check Name
		editor = map_Editor.get(MToDo.COLUMNNAME_Name);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_Name);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_MToDo.setName((String)editor.getValue());
		}

		//Check Description
		editor = map_Editor.get(MToDo.COLUMNNAME_Description);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{

		}else {
			p_MToDo.setDescription(editor.getValue().toString());
		}

		//Check Comments
		editor = map_Editor.get(MToDo.COLUMNNAME_Comments);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{

		}else {
			p_MToDo.setComments(editor.getValue().toString());
		}

		//Check JP_ToDo_ScheduledStartTime
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
		if(editor.getValue() == null)
		{
			if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
				throw new WrongValueException(editor.getComponent(), msg);
			}

		}else {
			p_MToDo.setJP_ToDo_ScheduledStartTime((Timestamp)editor.getValue());
		}

		//Check JP_ToDo_ScheduledEndTime
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
		if(editor.getValue() == null)
		{
			if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type) || MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
				throw new WrongValueException(editor.getComponent(), msg);
			}

		}else {
			p_MToDo.setJP_ToDo_ScheduledEndTime((Timestamp)editor.getValue());
		}


		//Check JP_ToDo_Status
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_Status);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_MToDo.setJP_ToDo_Status(editor.getValue().toString());
		}

		//Check IsOpenToDoJP
		editor = map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP);
		p_MToDo.setIsOpenToDoJP(((boolean)editor.getValue()));

		//Check JP_Statistics_YesNo
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo);
		p_MToDo.setJP_Statistics_YesNo(((String)editor.getValue()));

		//Check JP_Statistics_Choice
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice);
		p_MToDo.setJP_Statistics_Choice(((String)editor.getValue()));

		//Check JP_Statistics_DateAndTime
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime);
		p_MToDo.setJP_Statistics_DateAndTime(((Timestamp)editor.getValue()));

		//Check JP_Statistics_Number
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number);
		p_MToDo.setJP_Statistics_Number(((BigDecimal)editor.getValue()));

		String msg = p_MToDo.beforeSavePreCheck(true);
		if(!Util.isEmpty(msg))
		{
			FDialog.error(0, this, msg);
			return false;
		}

		if (p_MToDo.save())
		{
			if (log.isLoggable(Level.FINE)) log.fine("JP_ToDo_ID=" + p_MToDo.getJP_ToDo_ID());

			p_IsDirty = false;
			p_RequeryOnCancel = true;

			updateControlParameter(p_MToDo.getJP_ToDo_ID());
			updateEditorValue();
			updateEditorStatus();
			updateNorth();
			updateCenter();

		}
		else
		{
			FDialog.error(0, this, Msg.getMsg(ctx, "SaveError") + " : "+ Msg.getMsg(ctx, "JP_UnexpectedError"));
			return false;
		}

		return true;
	}

	private boolean deleteToDo()
	{
		p_MToDo.delete(false);
		p_MToDo = null;
		p_TeamMToDo = null;
		list_ToDoes.remove(index);
		p_RequeryOnCancel = true;

		if(index >= list_ToDoes.size())
			index--;

		if(index >= 0 && list_ToDoes.size() > 0)
		{
			p_IsDirty = false;
			updateControlParameter(list_ToDoes.get(index).getJP_ToDo_ID());
			updateWindowTitle();
			updateEditorValue();
			updateEditorStatus();
			updateNorth();
			updateCenter();

		}else {

			i_CallerPersonalToDoPopupwindow.refresh(p_JP_ToDo_Type);
			this.detach();
		}

		return true;
	}

	@Override
	public void valueChange(ValueChangeEvent evt)//TODo
	{
		String name = evt.getPropertyName();
		Object value = evt.getNewValue();
		Object source = evt.getSource();

		if(MToDo.COLUMNNAME_JP_ToDo_Type.equals(name))
		{
			if(value != null)
				p_JP_ToDo_Type = (String)value;

			updateCenter();

		}else if(MToDo.COLUMNNAME_AD_User_ID.equals(name)) {

			String validationCode = null;
			if(evt.getNewValue()==null)
			{
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL";
			}else {
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + (Integer)evt.getNewValue();
			}

			MLookup JP_ToDo_Category_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Category_ID),  DisplayType.Search);
			JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
			WSearchEditor editor_JP_ToDo_Category_ID = new WSearchEditor(JP_ToDo_Category_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID), null, true, p_IsNewRecord? false : true, true);
			map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Category_ID,editor_JP_ToDo_Category_ID);
			updateCenter();

		}

		p_IsDirty = true;
		updateNorth();
	}

	@Override
	public void onClose()
	{
		if(p_IsDirty)
		{

			FDialog.ask(0, null, "SaveChanges?", new Callback<Boolean>() {//Do you want to save changes?

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						if(!saveToDo())
							return ;

					}else{
						;
					}

					if(p_RequeryOnCancel)
					{
						i_CallerPersonalToDoPopupwindow.refresh(p_JP_ToDo_Type);
					}
					detach();
		        }

			});//FDialog.

		}else {

			if(p_RequeryOnCancel)
			{
				i_CallerPersonalToDoPopupwindow.refresh(p_JP_ToDo_Type);
			}
			detach();
		}


	}


}
