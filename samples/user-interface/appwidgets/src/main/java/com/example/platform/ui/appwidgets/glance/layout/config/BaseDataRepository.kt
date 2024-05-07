package com.example.layoutsamples.config

import android.content.Context

interface BaseDataRepository {
  /**
   * Selects a data source for presenting the data (used by configuration activity).
   */
  suspend fun selectDataSource(context: Context, dataSourceId: String)
}