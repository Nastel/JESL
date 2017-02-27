REM Run this file from the dos prompt. You will need the command line simulator. This batch program will change the dates on your computer to simulate in bulk.  

FOR /L %%M IN (1,1,12) DO (
  FOR /L %%D IN (1,1,28) DO (
    FOR /L %%H IN (0,1,23) DO (
      date %%M/%%D/2017
      time %%H:00:00
      jksim -A:<your-token> -f:<your-simulator-file> -i:2
    )
  )
)