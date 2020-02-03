-- Molly the Poly
-- 1.2.0 @markeats
-- llllllll.co/t/molly-the-poly/
--
-- MIDI or grid controlled classic
-- polysynth with patch creator.
--
-- E1 : Choose a patch planet
-- E2 : Create a sound
--

local MusicUtil = require "musicutil"
local MollyThePoly = include("lib/molly_the_poly_engine")

engine.name = "MollyThePoly"

local SCREEN_FRAMERATE = 15
local screen_dirty = true

local midi_in_device
local grid_device

local SUN_BASE_RADIUS = 5
local sun_mod_radius = 0
local sun_cool_down = false

local explosions = {}

local PLANET_RADIUS = 2.8
local planets = {{name = "Lead"}, {name = "Pad"}, {name = "Perc"}}
local selected_planet = 1
local selected_planet_id = 1

local stars = {}


local function add_star(id)
  local size = math.random(1, 3)
  local star = {x = math.random(2, 126), y = math.random(2, 62), width = math.max(3, size * 2 - 1), height = 1 + size * 2}
  local distance_from_sun = math.sqrt(math.pow(math.abs(star.x - 64), 2) + math.pow(math.abs(star.y - 32), 2))
  if distance_from_sun < 35 then
    if star.x > 64 then
      star.x = star.x + 35
    else
      star.x = star.x - 35
    end
    star.x = util.clamp(star.x, 5, 123)
  end
  stars[id] = star
end

local function remove_star(id)
  stars[id] = nil
end

local function remove_all_stars()
  stars = {}
end

local function add_explosion(planet_id, radius)
  table.insert(explosions, {planet_id = planet_id, x = 64, y = 32, radius = radius, velocity = 2, life = 0.66})
end

local function randomize()
  
  MollyThePoly.randomize_params(planets[selected_planet_id].name:lower())
  
  add_explosion(nil, SUN_BASE_RADIUS + sun_mod_radius)
  add_explosion(selected_planet_id, PLANET_RADIUS)
end

local function note_on(note_num, vel)
  engine.noteOn(note_num, MusicUtil.note_num_to_freq(note_num), vel)
  add_star(note_num)
end

local function note_off(note_num)
  engine.noteOff(note_num)
  remove_star(note_num)
end

local function note_off_all()
  engine.noteOffAll()
  remove_all_stars()
end

local function note_kill_all()
  engine.noteKillAll()
  remove_all_stars()
end

local function set_pressure(note_num, pressure)
  engine.pressure(note_num, pressure)
end

local function set_pressure_all(pressure)
  engine.pressureAll(pressure)
end

local function set_timbre(note_num, timbre)
  engine.timbre(note_num, timbre)
end

local function set_timbre_all(timbre)
  engine.timbreAll(timbre)
end

local function set_pitch_bend(note_num, bend_st)
  engine.pitchBend(note_num, MusicUtil.interval_to_ratio(bend_st))
end

local function set_pitch_bend_all(bend_st)
  engine.pitchBendAll(MusicUtil.interval_to_ratio(bend_st))
end


-- Encoder input
function enc(n, delta)
  
  if n == 2 then
    selected_planet = util.clamp(selected_planet + util.clamp(-1, 1, delta) * 0.15, 1, #planets)
    selected_planet_id = util.round(selected_planet)
    
  elseif n == 3 then
    
    if not sun_cool_down then
      if delta < 0 then
        if sun_mod_radius > 1 then
          delta = 0
        else
          delta = delta * 0.1
        end
      else
        delta = delta * util.linlin(0, 28, util.linlin(1, 3, 0.5, 1.3, selected_planet_id), 1.6, sun_mod_radius)
      end
      sun_mod_radius = util.clamp(sun_mod_radius + delta, SUN_BASE_RADIUS * - 0.5, planets[selected_planet_id].orbit + 2)
    end
  
  end
end

-- Key input
function key(n, z)
  if z == 1 then
    if n == 2 then
      
    elseif n == 3 then
      
    end
  end
end

-- MIDI input
local function midi_event(data)
  
  local msg = midi.to_msg(data)
  local channel_param = params:get("midi_channel")
  
  if channel_param == 1 or (channel_param > 1 and msg.ch == channel_param - 1) then
    
    -- Note off
    if msg.type == "note_off" then
      note_off(msg.note)
      print("note off", msg.note)
    
    -- Note on
    elseif msg.type == "note_on" then
      note_on(msg.note, msg.vel / 127)
      print("note on", msg.note)
      
    -- Pressure
    elseif msg.type == "key_pressure" then
      set_pressure(msg.note, msg.val / 127)
      
    -- Pressure all
    elseif msg.type == "channel_pressure" then
      set_pressure_all(msg.val / 127)
      
    -- Pitch bend all
    elseif msg.type == "pitchbend" then
      local bend_st = (util.round(msg.val / 2)) / 8192 * 2 -1 -- Convert to -1 to 1
      set_pitch_bend_all(bend_st * params:get("bend_range"))
      
    -- CC
    elseif msg.type == "cc" then
      
      -- Mod wheel / Timbre all
      if msg.cc == 1 then
        set_timbre_all(msg.val / 127)
        
      -- MPE X / Pitch bend
      -- TODO input from MPE X axis
      -- elseif msg.cc == ?? then
        -- local bend_st = (util.round(msg.val / 2)) / 8192 * 2 -1 -- Convert to -1 to 1
        -- set_pitch_bend(msg.note, bend_st * params:get("bend_range"))
      
      -- MPE Y / Timbre
      -- TODO input from MPE Y axis
      -- elseif msg.cc == ?? then
        -- set_timbre(msg.note, msg.val / 127)
        
      end
      
    end
  
  end
  
end

-- Grid input
local function grid_key(x, y, z)
  local note_num = util.clamp(((7 - y) * 5) + x + 33, 0, 127)
  
  if z == 1 then
    note_on(note_num, 0.8)
    grid_device:led(x, y, 15)
  else
    note_off(note_num)
    grid_device:led(x, y, 0)
  end
  grid_device:refresh()
end


local function solar_system_update()
  
  if not sun_cool_down and SUN_BASE_RADIUS + sun_mod_radius > planets[selected_planet_id].orbit + 1 then
    randomize()
    sun_cool_down = true
    
  else
    if sun_cool_down then
      sun_mod_radius = sun_mod_radius * 0.5
    elseif sun_mod_radius > 0 then
      sun_mod_radius = sun_mod_radius * 0.9
    elseif sun_mod_radius < 0 then
      sun_mod_radius = sun_mod_radius + 0.15
    end
    
    if sun_mod_radius < 1 then sun_cool_down = false end
  end
  
  for i = 1, #planets do
    planets[i].position = (planets[i].position + planets[i].velocity) % (math.pi * 2)
    planets[i].x = 64 + planets[i].orbit * math.cos(planets[i].position)
    planets[i].y = 32 + planets[i].orbit * math.sin(planets[i].position)
  end
  
  for i = #explosions, 1, -1 do
    if explosions[i].planet_id then
      explosions[i].x = planets[explosions[i].planet_id].x
      explosions[i].y = planets[explosions[i].planet_id].y
    end
    explosions[i].radius = explosions[i].radius + explosions[i].velocity
    explosions[i].velocity = explosions[i].velocity * 0.93
    explosions[i].life = explosions[i].life - 1 / SCREEN_FRAMERATE
    if explosions[i].life <= 0 then
      table.remove(explosions, i)
    end
  end
  
  screen_dirty = true
end


function init()
  
  midi_in_device = midi.connect(1)
  midi_in_device.event = midi_event
  
  grid_device = grid.connect(1)
  grid_device.key = grid_key
  
  -- Add params
  
  params:add{type = "number", id = "midi_device", name = "MIDI Device", min = 1, max = 4, default = 1, action = function(value)
    midi_in_device.event = nil
    midi_in_device = midi.connect(value)
    midi_in_device.event = midi_event
  end}
  
  local channels = {"All"}
  for i = 1, 16 do table.insert(channels, i) end
  params:add{type = "option", id = "midi_channel", name = "MIDI Channel", options = channels}

  params:add{type = "number", id = "bend_range", name = "Pitch Bend Range", min = 1, max = 48, default = 2}
  
  params:add{type = "number", id = "grid_device", name = "Grid Device", min = 1, max = 4, default = 1,
    action = function(value)
      grid_device:all(0)
      grid_device:refresh()
      grid_device.key = nil
      grid_device = grid.connect(value)
      grid_device.key = grid_key
      grid_dirty = true
    end}
  
  params:add_separator()
  
  MollyThePoly.add_params()
  
  local orbit = 13.5
  for i = 1, #planets do
    planets[i].orbit = orbit
    planets[i].position = math.random() * math.pi * 2
    planets[i].velocity = util.linlin(0, 1, 0.01, 0.03, math.random())
    orbit = orbit + 8
  end
  
  local screen_refresh_metro = metro.init()
  screen_refresh_metro.event = function()
    solar_system_update()
    if screen_dirty then
      screen_dirty = false
      redraw()
    end
  end
  
  solar_system_update()
  
  screen_refresh_metro:start(1 / SCREEN_FRAMERATE)
end


local function dashed_circle(x, y, radius, dash_length, gap_length)
  
  local circum = 2 * math.pi * radius
  local segments = util.round(circum / (dash_length + gap_length))
  local segment_angle = math.pi * 2 / segments
  local dash_angle = segment_angle * (dash_length / (dash_length + gap_length))
  
  local start_angle = 0
  while start_angle < math.pi * 2 do
    screen.arc(64, 32, radius, start_angle, start_angle + dash_angle)
    screen.stroke()
    start_angle = start_angle + segment_angle
  end
end

function redraw()
  screen.clear()
  screen.aa(1)
  
  -- Explosions
  for i = 1, #explosions do
    screen.level(util.round(util.linexp(0, 1, 2, 15, explosions[i].life)))
    screen.circle(explosions[i].x, explosions[i].y, explosions[i].radius)
    screen.stroke()
  end
  
  -- Stars
  screen.level(3)
  for _, star in pairs(stars) do
    screen.rect(star.x - math.floor(star.width * 0.5), star.y, star.width, 1)
    screen.rect(star.x, star.y - math.floor(star.height * 0.5), 1, star.height)
  end
  screen.fill()
  
  -- Planets
  for i = 1, #planets do
    
    if i == selected_planet_id then
      -- Path
      screen.line_width(0.7)
      screen.level(5)
      screen.circle(64, 32, planets[i].orbit)
      screen.stroke()
      
      -- Planet outline
      screen.level(15)
      screen.circle(planets[i].x, planets[i].y, 5.5)
      screen.line_width(0.7)
      screen.stroke()
      
    else
      -- Path
      screen.line_width(1)
      screen.level(3)
      dashed_circle(64, 32, planets[i].orbit, 3, 3)
      
      screen.level(4)
    end
    
    -- Planet
    screen.circle(planets[i].x, planets[i].y, PLANET_RADIUS)
    screen.fill()
    
  end
  screen.line_width(1)
  
  -- Sun
  screen.circle(64, 32, SUN_BASE_RADIUS + sun_mod_radius)
  screen.level(15)
  screen.fill()
  
  -- Label
  screen.move(3, 58)
  screen.level(15)
  screen.text(planets[selected_planet_id].name)
  screen.fill()
  
  
  screen.update()
end
