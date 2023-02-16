package com.app.tiniva.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.tiniva.ModelClass.TaskDetailsApi.TaskList;
import com.app.tiniva.ModelClass.TaskDetailsApi.TaskUpdatedList;
import com.app.tiniva.R;
import com.app.tiniva.Utils.LoginPrefManager;

import java.util.List;

public class TasksListUpdatedAdapter extends RecyclerView.Adapter<TasksListUpdatedAdapter.MyViewHolder> {

    private Context context;
    private List<TaskUpdatedList> tasksList;

    private LoginPrefManager loginPrefManager;

    public TasksListUpdatedAdapter(Context mContext, List<TaskUpdatedList> tasksList) {
        this.context = mContext;
        this.tasksList = tasksList;
        loginPrefManager = new LoginPrefManager(context);
    }

    @NonNull
    @Override
    public TasksListUpdatedAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tasks_list_updated_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TasksListUpdatedAdapter.MyViewHolder holder, int position) {
        holder.rvTasks.setLayoutManager(new LinearLayoutManager(context));
        holder.tasksListAdapter = new TasksListAdapter(context,tasksList.get(position).getTaskDetails(),taskListListener);
        holder.rvTasks.setAdapter(holder.tasksListAdapter);
    }

    @Override
    public int getItemCount() {
        return tasksList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvTasks;
        CardView card_view_list_item;
        TasksListAdapter tasksListAdapter;


        MyViewHolder(View view) {
            super(view);
            card_view_list_item = view.findViewById(R.id.card_view_list_item);
            rvTasks = view.findViewById(R.id.rvTasks);

            /*card_view_list_item.setOnClickListener(view1 -> {
                final int position = getAdapterPosition();
                 if (taskListListener!=null){
                     taskListListener.taskItemClick(taskListInfoList.get(position));
                 }
            });*/
        }
    }
    private TasksListUpdatedListener taskListListener;

    public interface TasksListUpdatedListener {
        void taskItemClick(TaskList taskList);
    }


    public  void setAdapterClickInterface(TasksListUpdatedListener clickInterface){
        this.taskListListener =clickInterface;
    }
}
