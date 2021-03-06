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
package jpiere.plugin.groupware.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MMessage;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * JPIERE-0469: JPiere Groupware
 *
 * MToDo
 *
 * @author h.hagiwara
 *
 */
public class MToDo extends X_JP_ToDo {

	public MToDo(Properties ctx, int JP_ToDo_Team_ID, String trxName)
	{
		super(ctx, JP_ToDo_Team_ID, trxName);
	}


	public MToDo(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}


	@Override
	protected boolean beforeSave(boolean newRecord)
	{

		String msg = beforeSavePreCheck(newRecord);
		if(!Util.isEmpty(msg))
		{
			log.saveError("Error", msg);
			return false;
		}

		return true;
	}

	public String beforeSavePreCheck(boolean newRecord)
	{
		//** Check User**/
		if(!newRecord)
		{
			int loginUser  = Env.getAD_User_ID(getCtx());
			if(loginUser == getAD_User_ID() || loginUser == getCreatedBy())
			{
				;//Updatable

			}else{
				MMessage msg = MMessage.get(getCtx(), "AccessCannotUpdate");//You cannot update this record - You don't have the privileges
				return msg.get_Translation("MsgText") + " - "+ msg.get_Translation("MsgTip");
			}
		}


		setAD_Org_ID(0);

		//*** Check ToDo Type ***//
		if(Util.isEmpty(getJP_ToDo_Type()))
		{
			Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_Type)};
			return Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
		}


		//*** Check ToDo Category ***//
		if(getJP_ToDo_Category_ID() != 0 && (newRecord || is_ValueChanged(MToDoTeam.COLUMNNAME_JP_ToDo_Category_ID)))
		{
			if(MToDoCategory.get(getCtx(), getJP_ToDo_Category_ID()).getAD_User_ID() != 0 && MToDoCategory.get(getCtx(), getJP_ToDo_Category_ID()).getAD_User_ID() != getAD_User_ID() )
			{
				return Msg.getMsg(getCtx(), "JP_OtherUserToDoCategory");//You can't use other user's ToDo Category.
			}
		}

		//*** Check Schedule Time ***//
		if(newRecord || is_ValueChanged(MToDoTeam.COLUMNNAME_JP_ToDo_ScheduledStartTime) || is_ValueChanged(MToDoTeam.COLUMNNAME_JP_ToDo_ScheduledEndTime))
		{
			if(MToDo.JP_TODO_TYPE_Task.equals(getJP_ToDo_Type()))
			{
				if(getJP_ToDo_ScheduledEndTime() == null)
				{
					Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime)};
					return Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
				}

				setJP_ToDo_ScheduledStartTime(null);

			}if(MToDo.JP_TODO_TYPE_Schedule.equals(getJP_ToDo_Type())) {


				if(getJP_ToDo_ScheduledStartTime() == null)
				{
					Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime)};
					return Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
				}

				if(getJP_ToDo_ScheduledEndTime() == null)
				{
					Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime)};
					return Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
				}

				if(getJP_ToDo_ScheduledStartTime().after(getJP_ToDo_ScheduledEndTime()))
				{
					return Msg.getElement(getCtx(), "JP_ToDo_ScheduledStartTime") + " > " +  Msg.getElement(getCtx(), "JP_ToDo_ScheduledEndTime") ;
				}

			}if(MToDo.JP_TODO_TYPE_Memo.equals(getJP_ToDo_Type())) {

				setJP_ToDo_ScheduledStartTime(null);
				setJP_ToDo_ScheduledEndTime(null);
			}
		}


		//*** Check ToDo Status***//
		if(newRecord || is_ValueChanged(MToDoTeam.COLUMNNAME_JP_ToDo_Status))
		{
			if(Util.isEmpty(getJP_ToDo_Status()))
			{
				setJP_ToDo_Status(MToDo.JP_TODO_STATUS_NotYetStarted);
			}

			if(MToDoTeam.JP_TODO_STATUS_NotYetStarted.equals(getJP_ToDo_Status()))
			{
				setJP_ToDo_StartTime(null);
				setJP_ToDo_EndTime(null);
				setProcessed(false);

			}else if(MToDoTeam.JP_TODO_STATUS_WorkInProgress.equals(getJP_ToDo_Status())) {

				if(getJP_ToDo_StartTime() == null)
					setJP_ToDo_StartTime(new Timestamp(System.currentTimeMillis()));
				setJP_ToDo_EndTime(null);
				setProcessed(false);

			}else if(MToDoTeam.JP_TODO_STATUS_Completed.equals(getJP_ToDo_Status())) {

				if(getJP_ToDo_StartTime() == null)
					setJP_ToDo_StartTime(new Timestamp(System.currentTimeMillis()));
				setJP_ToDo_EndTime(new Timestamp(System.currentTimeMillis()));
				setProcessed(true);

				//*** Check Statistics info ***//
				if(getJP_ToDo_Team_ID() != 0)
				{
					MToDoTeam teamToDo = new MToDoTeam(getCtx(), getJP_ToDo_Team_ID(), get_TrxName());
					if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_None.equals(teamToDo.getJP_Mandatory_Statistics_Info()))
					{
						;//Noting to do;

					}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_YesNo.equals(teamToDo.getJP_Mandatory_Statistics_Info())){

						if(Util.isEmpty(getJP_Statistics_YesNo()))
						{
							String msg = Msg.getElement(getCtx(), MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info);
							Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_Statistics_YesNo)};
							return msg + " : " + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
						}

					}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Choice.equals(teamToDo.getJP_Mandatory_Statistics_Info())){

						if(Util.isEmpty(getJP_Statistics_Choice()))
						{
							String msg = Msg.getElement(getCtx(), MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info);
							Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_Statistics_Choice)};
							return msg + " : " + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
						}

					}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_DateAndTime.equals(teamToDo.getJP_Mandatory_Statistics_Info())){

						if(getJP_Statistics_DateAndTime() == null)
						{
							String msg = Msg.getElement(getCtx(), MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info);
							Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_Statistics_DateAndTime)};
							return msg + " : " + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
						}

					}else if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Number.equals(teamToDo.getJP_Mandatory_Statistics_Info())){

						if(get_Value("JP_Statistics_Number") == null)
						{
							String msg = Msg.getElement(getCtx(), MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info);
							Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_Statistics_Number)};
							return msg + " : " + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs);
						}
					}

				}//if(getJP_ToDo_Team_ID() != 0)
			}
		}//if(newRecord || is_ValueChanged(MToDoTeam.COLUMNNAME_JP_ToDo_Status))

		return null;
	}


	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		if(success && !newRecord)
		{
			if(MToDo.JP_TODO_STATUS_Completed.equals(getJP_ToDo_Status()))
			{
				MToDoReminder[] reminders = getReminders();
				for(int i = 0;  i < reminders.length; i++)
				{
					reminders[i].setProcessed(true);
					reminders[i].saveEx(get_TrxName());
				}

			}else {

				if(MToDo.JP_TODO_STATUS_Completed.equals(get_ValueOld(MToDo.COLUMNNAME_JP_ToDo_Status)))
				{
					MToDoReminder[] reminders = getReminders();
					for(int i = 0;  i < reminders.length; i++)
					{
						if(reminders[i].isConfirmed())
						{
							;
						}else {
							reminders[i].setProcessed(false);
							reminders[i].saveEx(get_TrxName());
						}

					}//for
				}
			}
		}

		return true;
	}


	@Override
	protected boolean beforeDelete()
	{

		String msg = beforeDeletePreCheck();
		if(!Util.isEmpty(msg))
		{
			log.saveError("Error", msg);
			return false;
		}

		return true;
	}

	public String beforeDeletePreCheck()
	{
		//** Check User**/
		int loginUser  = Env.getAD_User_ID(getCtx());
		if(loginUser == getAD_User_ID() || loginUser == getCreatedBy())
		{
			//Deleteable;

		}else{

			MMessage msg = MMessage.get(getCtx(), "AccessCannotUpdate");
			return msg.get_Translation("MsgText") + " - "+ msg.get_Translation("MsgTip");
		}


		return null;
	}


	/**
	 * getToDoReminder
	 */
	protected MToDoReminder[] m_ToDoReminders = null;

	public MToDoReminder[] getReminders()
	{
		return getReminders(false);
	}

	public MToDoReminder[] getReminders(boolean requery)
	{

		if (m_ToDoReminders != null && m_ToDoReminders.length >= 0 && !requery)	//	re-load
			return m_ToDoReminders;
		//

		StringBuilder whereClauseFinal = new StringBuilder("JP_ToDo_ID=? AND IsActive = 'Y'");

		//
		List<MToDoReminder> list = new Query(getCtx(), MToDoReminder.Table_Name, whereClauseFinal.toString(), get_TrxName())
										.setParameters(get_ID())
										//.setOrderBy(orderClause)
										.list();

		m_ToDoReminders = list.toArray(new MToDoReminder[list.size()]);

		return m_ToDoReminders;

	}

}
