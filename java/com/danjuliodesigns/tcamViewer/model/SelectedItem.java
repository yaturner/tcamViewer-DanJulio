package com.danjuliodesigns.tcamViewer.model;

import androidx.annotation.NonNull;

import com.danjuliodesigns.tcamViewer.viewholders.LibraryItemViewHolder;

public class SelectedItem {
    int sectionIndex;
    int posInSection;
    int posInAdapter;
    String path;
    LibraryItemViewHolder holder;

    public SelectedItem(final int sectionIndex, final int posInSection, final int posInAdapter,
                        @NonNull LibraryItemViewHolder holder) {
        this.sectionIndex = sectionIndex;
        this.posInSection = posInSection;
        this.posInAdapter = posInAdapter;
        this.holder = holder;
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public void setSectionIndex(int sectionIndex) {
        this.sectionIndex = sectionIndex;
    }

    public int getPosInSection() {
        return posInSection;
    }

    public void setPosInSection(int posInSection) {
        this.posInSection = posInSection;
    }

    public String getPath() {
        return holder.getImagePath();
    }

    public LibraryItemViewHolder getHolder() {
        return holder;
    }

    public void setHolder(LibraryItemViewHolder holder) {
        this.holder = holder;
    }

    public int getPosInAdapter() {
        return posInAdapter;
    }

    public void setPosInAdapter(int posInAdapter) {
        this.posInAdapter = posInAdapter;
    }
}