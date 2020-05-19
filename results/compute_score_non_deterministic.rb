#!/usr/bin/ruby

def usage
  puts  "Usage: " + $0 + " FILE"
end

# We must have at least a file name
if ARGV.length == 0
  usage
  exit
end

# Open file
file = File.open(ARGV[0])

# arrays
wins      = 0
ties      = 0
losses    = 0

# For each line in file
file.each do |line|
  # Good lines to parse start with |*
  if line.start_with?("GLOBAL RESULTS")
    line = file.readline()
    line = file.readline()
    wins = wins + line.split(', ')[1].to_i
    line = file.readline()
    line = file.readline()
    line = file.readline()
    ties = ties + line.split(', ')[1].to_i
    line = file.readline()
    line = file.readline()
    line = file.readline()
    losses = losses + line.split(', ')[1].to_i
  end
end

score = wins + (ties.to_f / 2)

puts "Wins: #{wins}"
puts "Ties: #{ties}"
puts "Losses: #{losses}"
puts "Score: #{score}"

exit
