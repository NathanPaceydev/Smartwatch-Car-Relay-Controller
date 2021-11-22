package com.controlanything.NCDTCPRelay;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ActionAdapter extends ArrayAdapter<ActionObject>{
	
	private List<ActionObject> items;
	private int layoutResourceId;
	private Context context;

	public ActionAdapter(Context context, int layoutResourceId, List<ActionObject> items){
		super(context, layoutResourceId, items);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.items = items;
	}
	
	private class ViewHolder{
		TextView textView1;
		TextView textView2;
	}

	
	

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(convertView == null){
			holder = new ViewHolder();
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			convertView = inflater.inflate(R.layout.action_item, null);
			holder.textView1 = (TextView)convertView.findViewById(R.id.commandTextView);
			holder.textView2 = (TextView)convertView.findViewById(R.id.delayTextView);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		holder.textView1.setText(items.get(position).getActionCommand());
		holder.textView2.setText(String.valueOf(items.get(position).getActionDelay()));
		return convertView;
	}

}
