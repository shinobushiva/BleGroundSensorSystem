package me.shingaki.blesensorgroundsystem;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogMapFragment extends Fragment {


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
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log_map, container, false);
    }

}
