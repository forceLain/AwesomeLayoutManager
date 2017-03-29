# AwesomeLayoutManager

This library contains a LayoutManager implementation with the help of which you can:
 - Draw your views in vertical and horizontal orientations
 - Make animated transition between vertical and horizontal mode
 - Draw your views in vertical orientation like a "previews" with cute scale effect
 - Scroll your views like a normal RecyclerView or like a ViewPager
 
[gif]

All you need is to create and setup AwesomeLayoutManager instance

```
layoutManager = new AwesomeLayoutManager();

//how much your views will be scaled
layoutManager.setScaleFactor(0.5f);

//beheave like a ViewPager or not
layoutManager.setPagination(true);

// how much an item view will be high in vertical mode
layoutManager.setPageHeightFactor(.7f);

// time of transition between vertical and horizontal mode
layoutManager.setTransitionDuration(450);

recyclerView.setLayoutManager(layoutManager);
```

You can scroll your views like a normal RecyclerView

```
layoutManager.setPagination(false);
```

[![No pagination](https://img.youtube.com/vi/1XiGXLEccas/0.jpg)](https://youtu.be/1XiGXLEccas "No pagination")

Also you can control the maximum height of views. 
For instance, make it small:

```
layoutManager.setPageHeightFactor(.7f);
```

[![Small height](https://img.youtube.com/vi/wezqVGMleXk/0.jpg)](https://www.youtube.com/watch?v=wezqVGMleXk "Small height")