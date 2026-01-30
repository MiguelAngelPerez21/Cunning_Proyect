package com.example.cunning_proyect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CommunityInfoFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_info, container, false);

        TextView tvDetail = view.findViewById(R.id.tvInfoDetail);
        if (getArguments() != null) {
            String name = getArguments().getString("COMM_NAME");
            tvDetail.setText("Estás viendo la información de: " + name);
        }
        return view;
    }
}