package me.shingaki.blesensorgroundsystem;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Mapsを利用してマーカーの登録/表示ができるフラグメント
 */
public class LogMapFragment extends Fragment implements OnMapReadyCallback {
    private final static String TAG = LogMapFragment.class.getSimpleName();

    private GoogleMap mMap;         // マップオブジェクト
    private EditText mEditTitle;    // 登録マーカーのタイトル入力
    private Button mSaveButton;     // 登録マーカー保存ボタン
    private ListView mPinListView;  // 下部のリスト

    private ArrayList<MarkerCircle> mMarkerList;    // リストビューに対応するマーカーリスト
    private List<ParseObject> mParseList;           // リストビューに対応するParseリスト
    private Marker mCurrentMarker;  // 登録マーカー

    private ParseService mParseService;
    private ProgressDialog mProgress;


    public LogMapFragment() {
        // Required empty public constructor
    }

    public static LogMapFragment newInstance() {
        LogMapFragment fragment = new LogMapFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        mParseService = ((MainActivity) getActivity()).mParseService;

        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage("Loading...");
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_log_map, container, false);

        mEditTitle = (EditText) view.findViewById(R.id.editPinTitle);
        mSaveButton = (Button) view.findViewById(R.id.pinSaveButton);
        mPinListView = (ListView) view.findViewById(R.id.pinListView);

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mEditTitle.getText().toString();
                if (text.length() == 0) {
                    Toast.makeText(getActivity(), "タイトルを入力", Toast.LENGTH_LONG).show();
                    return;
                }

                if (mCurrentMarker == null) {
                    Toast.makeText(getActivity(), "マップをタップしてマーカーを設置", Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    mProgress.show();
                    mParseService.uploadMarker(text, mCurrentMarker.getPosition(), new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            Log.d(TAG, "uploadMarker / done");

                            clearMarker();
                            mProgress.dismiss();
                            fetchList();
                        }
                    });
                } catch (ParseException e) {
                    mProgress.dismiss();
                    e.printStackTrace();
                }
            }
        });


        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

        return view;
    }

    private void clearMarker() {
        mEditTitle.setText("");
        mCurrentMarker.remove();
        mCurrentMarker = null;

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        fetchList();

        // マップクリックした場合登録マーカーを作成/移動する
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "タップ位置\n緯度：" + latLng.latitude + "\n経度:" + latLng.longitude);

                if (mCurrentMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).draggable(true).icon(getMarkerIcon("#ff2299"));
                    mCurrentMarker = mMap.addMarker(markerOptions);
                } else {
                    mCurrentMarker.setPosition(latLng);
                }

            }
        });

        // 登録用マーカーのドラッグ移動
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                Log.d(TAG, "onMarkerDragStart");
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                Log.d(TAG, "onMarkerDrag");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Log.d(TAG, "onMarkerDragEnd");
                mCurrentMarker = marker;
            }
        });

        // 登録用マーカーをクリックすると削除
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "onMarkerClick");

                if (marker.equals(mCurrentMarker)) {
                    mCurrentMarker.remove();
                    mCurrentMarker = null;
                }

                return false;
            }
        });

    }

    /**
     * 登録されたマーカーを取得する
     */
    private void fetchList() {
        mParseService.listMarker(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                mMarkerList = new ArrayList<>();
                mParseList = list;

                for (ParseObject o : list) {
                    String title = o.getString("title");
                    ParseGeoPoint pgo = o.getParseGeoPoint("latlng");
                    LatLng latLng = new LatLng(pgo.getLatitude(), pgo.getLongitude());
                    Marker marker = mMap.addMarker(createMarkerOptions(title, latLng));

                    mMarkerList.add(new MarkerCircle(marker));
                }

                updateListView();
            }
        });
    }

    /**
     * 下部のリストを生成
     */
    private void updateListView() {
        List<Map<String, String>> list = new ArrayList<>();

        for (MarkerCircle mc : mMarkerList) {
            Marker o = mc.getMarker();
            Map<String, String> map = new HashMap<>();
            map.put("title", o.getTitle());
            LatLng ll = o.getPosition();
            map.put("body", ll.latitude + " " + ll.longitude);
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(getActivity(),
                list,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "body"},
                new int[]{ android.R.id.text1, android.R.id.text2}
        );

        mPinListView.setAdapter(adapter);

        // 下部リストクリック時
        mPinListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mPinListView / onItemClick / " + position);
                MarkerCircle mc = mMarkerList.get(position);
                Marker m = mc.getMarker();
                LatLng ll = m.getPosition();

                if (mc.hasMapCircle() == false) {
                    CircleOptions circleOptions = new CircleOptions()
                            .center(ll)
                            .radius(1000)
                            .strokeColor(Color.argb(0xFF, 0x33, 0x99, 0xFF))
                            .strokeWidth(10.0f)
                            .fillColor(Color.argb(0x44, 0x33, 0x99, 0xFF));
                    Circle circle = mMap.addCircle(circleOptions);
                    mc.setCircle(circle);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(ll));

            }
        });

        // 長押しクリックで該当マーカー削除
        mPinListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mPinListView / onItemLongClick / " + position);

                final Context context = getActivity();
                final int index = position;

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setTitle("削除:マーカー");
                alertDialog.setMessage("選択したマーカーを削除します。");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "OK");
                        removeMarkerAndClear(index);
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Cancel");
                    }
                });
                alertDialog.show();

                return false;
            }
        });

    }

    /**
     * マーカー削除
     * @param index 削除するアイテムのリストインデックス
     */
    private void removeMarkerAndClear(int index) {
        Log.d(TAG, "removeMarkerAndClear / " + index);
        ParseObject po = mParseList.get(index);

        Log.d(TAG, po.toString());
        mProgress.show();
        po.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                mProgress.hide();

                if (e != null) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getActivity(), "削除に失敗しました", Toast.LENGTH_LONG).show();
                }

                mMap.clear();
                fetchList();
            }
        });
    }

    /**
     * マーカー作成
     * snippetは改行すると省略表示とされるため1行表示としている
     * @param title マーカークリック時に表示するタイトル
     * @param latLng マーカーを表示する緯度・経度
     * @return
     */
    private MarkerOptions createMarkerOptions(String title, LatLng latLng) {
        String snippet = String.format("lat:%6f lng:%6f", latLng.latitude, latLng.longitude);
        return new MarkerOptions().position(latLng).title(title).snippet(snippet).icon(getMarkerIcon("#00ff00"));
    }

    /**
     * マーカー用にColorオブジェクトを作成
     * @param color 16進数RGB
     * @return
     */
    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    private class MarkerCircle {

        private Marker marker;
        private Circle circle;

        public MarkerCircle(Marker marker) {
            this.marker = marker;
        }

        public Marker getMarker() {
            return marker;
        }

        public void setMarker(Marker marker) {
            this.marker = marker;
        }

        public Circle getCircle() {
            return circle;
        }

        public void setCircle(Circle circle) {
            this.circle = circle;
        }

        public boolean hasMapCircle() {
            return this.circle != null;
        }
    }
}
