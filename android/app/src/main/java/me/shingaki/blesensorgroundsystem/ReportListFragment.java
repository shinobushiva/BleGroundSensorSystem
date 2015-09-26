package me.shingaki.blesensorgroundsystem;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.JsonWriter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportListFragment extends Fragment {

    private final static String TAG = ReportListFragment.class.getSimpleName();

    ListView mListReports;

    private MainActivity activity;

    private List<ParseObject> currentItems;

    private ProgressDialog mProgress;

    private ParseService mParseService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.activity_report_list, container, false);
        activity = (MainActivity)getActivity();
        mParseService = activity.mParseService;

        String[] members = { "mhidaka", "rongon_xp", "kacchi0516", "kobashinG",
                "seit", "kei_i_t", "furusin_oriver" };

        mListReports = (ListView) view.findViewById(R.id.list_reports);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_expandable_list_item_1, members);

        mListReports.setAdapter(adapter);
        mListReports.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ListView listView = (ListView) parent;
                // クリックされたアイテムを取得します

                ParseObject po = currentItems.get(position);
                Date d = po.getDate("current_time");
                String place = po.getString("place");
                String weather = po.getString("weather");
                String oId = po.getObjectId();
                ParseGeoPoint pgp = po.getParseGeoPoint("location");

                Intent intent = new Intent();
                intent.setClassName(SensorReportViewActivity.class.getPackage().getName(), SensorReportViewActivity.class.getName());
                intent.putExtra("ObjectId", oId);
                startActivity(intent);

            }
        });
        // リストビューのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
        mListReports.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ListView listView = (ListView) parent;
                // 選択されたアイテムを取得します
                String item = (String) listView.getSelectedItem();
                Toast.makeText(activity, item, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        return view;

    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if(activity == null)
            return;

        if (visible) {

            mProgress = new ProgressDialog(activity);
            mProgress.setMessage("Loading...");
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show();

            mParseService.listSensorReport(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {

                    currentItems = list;

                    List<String> items = new ArrayList<String>();

                    for (ParseObject o : currentItems) {
                        Date d = o.getDate("current_time");
                        String place = o.getString("place");
                        items.add(place+":"+d);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_expandable_list_item_1, items);
                    mListReports.setAdapter(adapter);

                    mProgress.dismiss();
                }
            });
        }
    }


}
