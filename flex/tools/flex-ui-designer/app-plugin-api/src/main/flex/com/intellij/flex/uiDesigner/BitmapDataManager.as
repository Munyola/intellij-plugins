package com.intellij.flex.uiDesigner {
import flash.display.BitmapData;

public class BitmapDataManager {
  private const data:Vector.<BitmapData> = new Vector.<BitmapData>();

  public function get(id:int):BitmapData {
    return data[id];
  }

  public function register(id:int, bitmapData:BitmapData):void {
    assert(id == data.length || (id < data.length && data[id] == null));
    // BitmapDataManager is not exlusive owner of id, so, second assert (as in DocumentFactoryManager) is not needed
    // (due to server BinaryFileManager for both client BitmapDataManager and client SwfDataManager) 

    data[id] = bitmapData;
  }
}
}
